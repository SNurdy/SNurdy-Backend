package com.snurdy.backend.domain.migration.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.migration.dto.MigrationStartResponse
import com.snurdy.backend.domain.migration.dto.MigrationStatusResponse
import com.snurdy.backend.domain.migration.entity.MigrationSessionEntity
import com.snurdy.backend.domain.migration.entity.MigrationStatus
import com.snurdy.backend.domain.migration.repository.MigrationSessionRepository
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.constants.MigrationError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * 마이그레이션 세션의 라이프사이클과 상태 전이를 책임진다.
 * 데이터 페이로드 저장은 MigrationUploadService 에 위임한다.
 */
@Service
class MigrationSessionService(
    private val migrationSessionRepository: MigrationSessionRepository,
    private val userRepository: UserRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val migrationSessionFailureService: MigrationSessionFailureService,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock
) {

    private fun now(): Instant = clock.instant()

    @Transactional
    fun start(userId: UUID): MigrationStartResponse {
        val user = findUser(userId)
        if (user.migrationDeclinedAt != null) {
            throw ApiException(MigrationError.MIGRATION_DECLINED)
        }

        val existing = migrationSessionRepository.findByUserId(userId)
        if (existing != null) {
            if (existing.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)
            return MigrationStartResponse(migrationId = existing.id.toString())
        }

        if (hasExistingLearningData(userId)) {
            throw ApiException(MigrationError.MIGRATION_DATA_CONFLICT)
        }

        return try {
            val session = migrationSessionRepository.save(MigrationSessionEntity(user = user))
            MigrationStartResponse(migrationId = session.id.toString())
        } catch (_: DataIntegrityViolationException) {
            val conflicted = migrationSessionRepository.findByUserId(userId)
                ?: throw ApiException(MigrationError.MIGRATION_PROCESSING_ERROR)
            if (conflicted.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)
            MigrationStartResponse(migrationId = conflicted.id.toString())
        }
    }

    @Transactional
    fun decline(userId: UUID): MigrationStatusResponse {
        val user = findUser(userId)
        val session = migrationSessionRepository.findByUserId(userId)

        if (session?.isCompleted() == true) {
            return MigrationStatusResponse(
                status = MigrationStatus.COMPLETED.name,
                completedAt = session.completedAt
            )
        }

        if (user.migrationDeclinedAt == null) {
            user.migrationDeclinedAt = now()
        }

        return MigrationStatusResponse(status = MigrationStatus.DECLINED.name)
    }

    @Transactional
    fun finalize(userId: UUID, migrationId: String): MigrationStatusResponse {
        val user = findUser(userId)
        if (user.migrationDeclinedAt != null) {
            throw ApiException(MigrationError.MIGRATION_DECLINED)
        }

        val session = findSessionForUpdate(migrationId)
        if (session.user.id != userId) throw ApiException(MigrationError.MIGRATION_SESSION_FORBIDDEN)

        if (session.isCompleted()) {
            return MigrationStatusResponse(
                status = MigrationStatus.COMPLETED.name,
                completedAt = session.completedAt
            )
        }

        if (!session.isUploaded()) throw ApiException(MigrationError.MIGRATION_UPLOAD_REQUIRED)

        try {
            session.complete()
            migrationSessionRepository.save(session)
        } catch (e: ApiException) {
            scheduleFailureRecord(session.id, e.responseMessage)
            throw e
        } catch (e: Exception) {
            scheduleFailureRecord(session.id, MigrationError.MIGRATION_FINALIZE_INTERNAL_ERROR.message)
            throw e
        }

        return MigrationStatusResponse(
            status = MigrationStatus.COMPLETED.name,
            completedAt = session.completedAt
        )
    }

    @Transactional(readOnly = true)
    fun getStatus(userId: UUID): MigrationStatusResponse {
        val user = findUser(userId)

        if (user.migrationDeclinedAt != null) {
            return MigrationStatusResponse(status = MigrationStatus.DECLINED.name)
        }

        val session = migrationSessionRepository.findByUserId(userId)
            ?: return MigrationStatusResponse(status = MigrationStatus.NOT_STARTED.name)

        return MigrationStatusResponse(
            status = session.status.name,
            completedAt = session.completedAt,
            errorMessage = session.errorMessage
        )
    }

    /**
     * 같은 패키지의 MigrationUploadService 가 세션을 비관락으로 잡을 때 사용.
     * 외부에서 직접 호출하지 않는다.
     */
    internal fun findSessionForUpdate(migrationId: String): MigrationSessionEntity {
        val uuid = runCatching { UUID.fromString(migrationId) }
            .getOrElse { throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND) }
        return migrationSessionRepository.findByIdForUpdate(uuid)
            ?: throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND)
    }

    /**
     * 호출 시점의 메인 트랜잭션이 롤백된 직후 세션을 FAILED 로 기록.
     * 활성 트랜잭션이 없으면 동기로 즉시 기록한다.
     */
    internal fun scheduleFailureRecord(sessionId: UUID, reason: String) {
        val normalizedReason = reason.ifBlank { MigrationError.MIGRATION_PROCESSING_ERROR.message }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            migrationSessionFailureService.markFailed(sessionId, normalizedReason)
            return
        }

        eventPublisher.publishEvent(MigrationFailureEvent(sessionId = sessionId, reason = normalizedReason))
    }

    internal fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId) ?: throw ApiException(UserError.NOT_FOUND)

    private fun hasExistingLearningData(userId: UUID): Boolean =
        termProgressRepository.existsByUserId(userId) ||
            glossaryProgressRepository.existsByUserId(userId) ||
            bookmarkRepository.existsByIdUserId(userId) ||
            dailyProgressRepository.existsByIdUserId(userId)
}
