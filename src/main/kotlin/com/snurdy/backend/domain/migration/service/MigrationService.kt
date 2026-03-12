package com.snurdy.backend.domain.migration.service

import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.catalog.term.entity.TermEntity
import com.snurdy.backend.domain.catalog.term.repository.TermRepository
import com.snurdy.backend.domain.learning.entity.*
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.migration.dto.*
import com.snurdy.backend.domain.migration.entity.MigrationSessionEntity
import com.snurdy.backend.domain.migration.entity.MigrationStatus
import com.snurdy.backend.domain.migration.repository.MigrationSessionRepository
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.MigrationError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MigrationService(
    private val migrationSessionRepository: MigrationSessionRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val userRepository: UserRepository,
    private val termRepository: TermRepository,
    private val glossaryRepository: GlossaryRepository
) {

    @Transactional
    fun start(userId: UUID): MigrationStartResponse {
        val user = findUser(userId)

        // 동일 userId로 재호출 시 기존 migrationId 반환
        val existing = migrationSessionRepository.findByUserId(userId)
        if (existing != null) {
            // COMPLETED면 재시도 불가
            if (existing.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)
            return MigrationStartResponse(migrationId = existing.id.toString())
        }

        val session = migrationSessionRepository.save(
            MigrationSessionEntity(user = user)
        )
        return MigrationStartResponse(migrationId = session.id.toString())
    }

    @Transactional
    fun upload(userId: UUID, migrationId: String, request: MigrationUploadRequest): MigrationUploadResponse {
        val session = findSession(migrationId)
        if (session.user.id != userId) throw ApiException(CommonError.FORBIDDEN)
        if (session.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)

        // 멱등성: 이미 업로드 완료된 세션이면 저장 없이 즉시 반환
        if (session.isUploaded()) {
            return MigrationUploadResponse(
                migrationId = migrationId,
                termProgressCount = request.termProgresses.size,
                glossaryProgressCount = request.glossaryProgresses.size
            )
        }

        // 서버에 이미 학습 데이터 존재 시 409
        if (termProgressRepository.existsByUserId(userId)) {
            throw ApiException(MigrationError.MIGRATION_DATA_CONFLICT)
        }

        saveTermProgresses(userId, request.termProgresses)
        saveGlossaryProgresses(userId, request.glossaryProgresses)

        session.markUploaded()  // IN_PROGRESS → UPLOADED
        migrationSessionRepository.save(session)

        return MigrationUploadResponse(
            migrationId = migrationId,
            termProgressCount = request.termProgresses.size,
            glossaryProgressCount = request.glossaryProgresses.size
        )
    }

    @Transactional
    fun finalize(userId: UUID, migrationId: String): MigrationStatusResponse {
        val session = findSession(migrationId)
        if (session.user.id != userId) throw ApiException(CommonError.FORBIDDEN)
        if (session.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)

        // upload 완료(UPLOADED) 상태가 아니면 finalize 불가
        if (!session.isUploaded()) throw ApiException(MigrationError.MIGRATION_UPLOAD_REQUIRED)

        session.complete()
        migrationSessionRepository.save(session)

        return MigrationStatusResponse(
            status = MigrationStatus.COMPLETED.name,
            completedAt = session.completedAt
        )
    }

    @Transactional(readOnly = true)
    fun getStatus(userId: UUID): MigrationStatusResponse {
        val session = migrationSessionRepository.findByUserId(userId)
            ?: return MigrationStatusResponse(status = MigrationStatus.NOT_STARTED.name)

        return MigrationStatusResponse(
            status = session.status.name,
            completedAt = session.completedAt,
            errorMessage = session.errorMessage
        )
    }

    private fun saveTermProgresses(userId: UUID, payloads: List<TermProgressPayload>) {
        val user = findUser(userId)

        val glossaryIds = payloads.map { it.glossaryId }.distinct()
        val termIds = payloads.map { it.termId }.distinct()

        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id!! }
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id!! }

        val entities = payloads.mapNotNull { payload ->
            val glossary = glossaries[payload.glossaryId] ?: return@mapNotNull null
            val term = terms[payload.termId] ?: return@mapNotNull null

            TermProgressEntity(
                user = user,
                glossary = glossary,
                term = term,
                status = runCatching {
                    TermProgressStatus.valueOf(payload.status)
                }.getOrDefault(TermProgressStatus.NEW),
                easeFactor = payload.easeFactor.toBigDecimal(),
                intervalDays = payload.intervalDays,
                reviewCount = payload.reviewCount,
                lastReviewedAt = payload.lastReviewedAt,
                nextReviewAt = payload.nextReviewAt,
                clientGeneratedId = payload.clientGeneratedId,
                occurredAt = payload.occurredAt
            )
        }

        termProgressRepository.saveAll(entities)
    }

    private fun saveGlossaryProgresses(userId: UUID, payloads: List<GlossaryProgressPayload>) {
        val user = findUser(userId)

        val glossaryIds = payloads.map { it.glossaryId }.distinct()
        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id!! }

        val entities = payloads.mapNotNull { payload ->
            val glossary = glossaries[payload.glossaryId] ?: return@mapNotNull null

            GlossaryProgressEntity(
                id = GlossaryProgressId(userId = userId, glossaryId = payload.glossaryId),
                user = user,
                glossary = glossary,
                learnedCount = payload.learnedCount,
                lastLearnedAt = payload.lastLearnedAt
            )
        }

        glossaryProgressRepository.saveAll(entities)
    }

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId) ?: throw ApiException(CommonError.NOT_FOUND)

    private fun findSession(migrationId: String): MigrationSessionEntity {
        val uuid = runCatching { UUID.fromString(migrationId) }
            .getOrElse { throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND) }
        return migrationSessionRepository.findByIdOrNull(uuid)
            ?: throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND)
    }
}