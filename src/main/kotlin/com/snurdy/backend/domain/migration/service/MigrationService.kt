package com.snurdy.backend.domain.migration.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.bookmark.entity.BookmarkEntity
import com.snurdy.backend.domain.bookmark.entity.BookmarkId
import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.learning.entity.DailyProgressEntity
import com.snurdy.backend.domain.learning.entity.DailyProgressId
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.learning.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.learning.entity.GlossaryProgressId
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.migration.dto.BookmarkPayload
import com.snurdy.backend.domain.migration.dto.DailyProgressPayload
import com.snurdy.backend.domain.migration.dto.GlossaryProgressPayload
import com.snurdy.backend.domain.migration.dto.MigrationStartResponse
import com.snurdy.backend.domain.migration.dto.MigrationStatusResponse
import com.snurdy.backend.domain.migration.dto.MigrationUploadRequest
import com.snurdy.backend.domain.migration.dto.MigrationUploadResponse
import com.snurdy.backend.domain.migration.dto.TermProgressPayload
import com.snurdy.backend.domain.migration.dto.UserSummaryPayload
import com.snurdy.backend.domain.migration.entity.MigrationSessionEntity
import com.snurdy.backend.domain.migration.entity.MigrationStatus
import com.snurdy.backend.domain.migration.repository.MigrationSessionRepository
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.MigrationError
import com.snurdy.backend.global.exception.ApiException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class MigrationService(
    private val migrationSessionRepository: MigrationSessionRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val userRepository: UserRepository,
    private val termRepository: TermRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val glossaryRepository: GlossaryRepository,
    private val migrationSessionFailureService: MigrationSessionFailureService
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
                ?: throw ApiException(CommonError.INTERNAL_ERROR)
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
            user.migrationDeclinedAt = Instant.now()
        }

        return MigrationStatusResponse(status = MigrationStatus.DECLINED.name)
    }

    @Transactional
    fun upload(userId: UUID, migrationId: String, request: MigrationUploadRequest): MigrationUploadResponse {
        validateDuplicateEntries(request)

        val session = findSessionForUpdate(migrationId)
        if (session.user.id != userId) throw ApiException(CommonError.FORBIDDEN)

        if (session.isUploaded() || session.isCompleted()) {
            return loadUploadCounts(migrationId, userId)
        }

        if (!session.isInProgress() && !session.isFailed()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_STATUS)
        }
        if (session.isFailed()) {
            session.retry()
            migrationSessionRepository.save(session)
        }

        return try {
            if (hasExistingLearningData(userId)) {
                throw ApiException(MigrationError.MIGRATION_DATA_CONFLICT)
            }

            val user = findUser(userId)
            val savedTermCount = saveTermProgresses(user, request.termProgresses)
            val savedGlossaryCount = saveGlossaryProgresses(user, request.glossaryProgresses)
            val savedBookmarkCount = saveBookmarks(user, request.bookmarks)
            val savedDailyProgressCount = saveDailyProgresses(user, request.dailyProgresses)
            applyUserSummary(user, request.userSummary)

            session.markUploaded()
            migrationSessionRepository.save(session)

            MigrationUploadResponse(
                migrationId = migrationId,
                termProgressCount = savedTermCount,
                glossaryProgressCount = savedGlossaryCount,
                bookmarkCount = savedBookmarkCount,
                dailyProgressCount = savedDailyProgressCount
            )
        } catch (e: ApiException) {
            scheduleFailureRecordAfterRollback(session.id, e.responseMessage)
            throw e
        } catch (e: Exception) {
            scheduleFailureRecordAfterRollback(session.id, "마이그레이션 업로드 처리 중 서버 오류가 발생했습니다")
            throw e
        }
    }

    @Transactional
    fun finalize(userId: UUID, migrationId: String): MigrationStatusResponse {
        val session = findSessionForUpdate(migrationId)
        if (session.user.id != userId) throw ApiException(CommonError.FORBIDDEN)

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
            scheduleFailureRecordAfterRollback(session.id, e.responseMessage)
            throw e
        } catch (e: Exception) {
            scheduleFailureRecordAfterRollback(session.id, "마이그레이션 완료 처리 중 서버 오류가 발생했습니다")
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
        val session = migrationSessionRepository.findByUserId(userId)
        if (session == null) {
            if (user.migrationDeclinedAt != null) {
                return MigrationStatusResponse(status = MigrationStatus.DECLINED.name)
            }
            return MigrationStatusResponse(status = MigrationStatus.NOT_STARTED.name)
        }

        return MigrationStatusResponse(
            status = session.status.name,
            completedAt = session.completedAt,
            errorMessage = session.errorMessage
        )
    }

    private fun saveTermProgresses(user: UserEntity, payloads: List<TermProgressPayload>): Int {
        if (payloads.isEmpty()) return 0

        val glossaryIds = payloads.map { it.glossaryId }.distinct()
        val termIds = payloads.map { it.termId }.distinct()

        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id!! }
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id!! }

        val missingGlossaryIds = glossaryIds.filterNot(glossaries::containsKey)
        if (missingGlossaryIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)
        }

        val missingTermIds = termIds.filterNot(terms::containsKey)
        if (missingTermIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_TERM_ID)
        }

        validateGlossaryTermPairs(
            termIds = termIds,
            pairs = payloads.map { it.termId to it.glossaryId }
        )

        val migratedAt = Instant.now()
        val entities = payloads.map { payload ->
            val glossary = glossaries[payload.glossaryId]!!
            val term = terms[payload.termId]!!
            val reviewCount = payload.reviewCount.coerceAtLeast(0)
            val intervalDays = payload.intervalDays.coerceAtLeast(0)
            val lastReviewedAt = payload.lastReviewedAt ?: if (reviewCount > 0) migratedAt else null

            TermProgressEntity(
                user = user,
                glossary = glossary,
                term = term,
                status = parseTermProgressStatus(payload.status),
                easeFactor = normalizeEaseFactor(payload.easeFactor),
                intervalDays = intervalDays,
                reviewCount = reviewCount,
                lastReviewedAt = lastReviewedAt,
                nextReviewAt = payload.nextReviewAt,
                clientGeneratedId = null,
                occurredAt = migratedAt
            )
        }

        termProgressRepository.saveAll(entities)
        return entities.size
    }

    private fun saveGlossaryProgresses(user: UserEntity, payloads: List<GlossaryProgressPayload>): Int {
        if (payloads.isEmpty()) return 0

        val glossaryIds = payloads.map { it.glossaryId }.distinct()
        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id!! }

        val missingGlossaryIds = glossaryIds.filterNot(glossaries::containsKey)
        if (missingGlossaryIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)
        }

        val entities = payloads.map { payload ->
            val glossary = glossaries[payload.glossaryId]!!

            GlossaryProgressEntity(
                id = GlossaryProgressId(userId = user.id, glossaryId = payload.glossaryId),
                user = user,
                glossary = glossary,
                learnedCount = payload.learnedCount,
                lastLearnedAt = payload.lastLearnedAt
            )
        }

        glossaryProgressRepository.saveAll(entities)
        return entities.size
    }

    private fun saveBookmarks(user: UserEntity, payloads: List<BookmarkPayload>): Int {
        if (payloads.isEmpty()) return 0

        val glossaryIds = payloads.map { it.glossaryId }.distinct()
        val termIds = payloads.map { it.termId }.distinct()

        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id!! }
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id!! }

        val missingGlossaryIds = glossaryIds.filterNot(glossaries::containsKey)
        if (missingGlossaryIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)
        }

        val missingTermIds = termIds.filterNot(terms::containsKey)
        if (missingTermIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_TERM_ID)
        }

        validateGlossaryTermPairs(
            termIds = termIds,
            pairs = payloads.map { it.termId to it.glossaryId }
        )

        val entities = payloads.map { payload ->
            val glossary = glossaries[payload.glossaryId]!!
            val term = terms[payload.termId]!!
            BookmarkEntity(
                id = BookmarkId(
                    userId = user.id,
                    glossaryId = payload.glossaryId,
                    termId = payload.termId
                ),
                user = user,
                glossary = glossary,
                term = term
            )
        }

        bookmarkRepository.saveAll(entities)
        return entities.size
    }

    private fun saveDailyProgresses(user: UserEntity, payloads: List<DailyProgressPayload>): Int {
        if (payloads.isEmpty()) return 0

        val entities = payloads.map { payload ->
            DailyProgressEntity(
                id = DailyProgressId(userId = user.id, date = payload.date),
                user = user,
                learnedCount = payload.learnedCount.coerceAtLeast(0),
                reviewedCount = payload.reviewedCount.coerceAtLeast(0)
            )
        }
        dailyProgressRepository.saveAll(entities)
        return entities.size
    }

    private fun applyUserSummary(user: UserEntity, payload: UserSummaryPayload?) {
        if (payload == null) return

        val currentStreak = payload.currentStreak.coerceAtLeast(0)
        val longestStreak = payload.longestStreak.coerceAtLeast(0)

        user.currentStreak = currentStreak
        user.longestStreak = maxOf(longestStreak, currentStreak)
        user.totalLearningTerms = payload.totalLearningTerms.coerceAtLeast(0)
    }

    private fun validateGlossaryTermPairs(
        termIds: List<Long>,
        pairs: List<Pair<Long, Long>>
    ) {
        if (pairs.isEmpty()) return

        val validPairs = glossaryTermRepository
            .findGlossariesByTermIds(termIds)
            .map { it.termId to it.glossaryId }
            .toSet()

        val hasInvalidPair = pairs.any { it !in validPairs }
        if (hasInvalidPair) {
            throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_TERM_PAIR)
        }
    }

    private fun validateDuplicateEntries(request: MigrationUploadRequest) {
        val duplicatedTermKeys = request.termProgresses
            .groupingBy { it.glossaryId to it.termId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedTermKeys.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_DUPLICATED_TERM_PROGRESS)
        }

        val duplicatedGlossaryIds = request.glossaryProgresses
            .groupingBy { it.glossaryId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedGlossaryIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_DUPLICATED_GLOSSARY_PROGRESS)
        }

        val duplicatedBookmarkKeys = request.bookmarks
            .groupingBy { it.glossaryId to it.termId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedBookmarkKeys.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_DUPLICATED_BOOKMARK)
        }

        val duplicatedDates = request.dailyProgresses
            .groupingBy(DailyProgressPayload::date)
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedDates.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_DUPLICATED_DAILY_PROGRESS)
        }
    }

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId) ?: throw ApiException(CommonError.NOT_FOUND)

    private fun findSessionForUpdate(migrationId: String): MigrationSessionEntity {
        val uuid = runCatching { UUID.fromString(migrationId) }
            .getOrElse { throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND) }
        return migrationSessionRepository.findByIdForUpdate(uuid)
            ?: throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND)
    }

    private fun parseTermProgressStatus(status: String): TermProgressStatus =
        runCatching { TermProgressStatus.valueOf(status.trim().uppercase()) }
            .getOrElse { throw ApiException(MigrationError.MIGRATION_INVALID_TERM_STATUS) }

    private fun normalizeEaseFactor(value: Double): BigDecimal =
        value
            .coerceAtLeast(MIN_EASE_FACTOR)
            .toBigDecimal()
            .setScale(2, RoundingMode.HALF_UP)

    private fun hasExistingLearningData(userId: UUID): Boolean =
        termProgressRepository.existsByUserId(userId) ||
            glossaryProgressRepository.existsByUserId(userId) ||
            bookmarkRepository.existsByIdUserId(userId) ||
            dailyProgressRepository.existsByIdUserId(userId)

    private fun loadUploadCounts(migrationId: String, userId: UUID): MigrationUploadResponse =
        MigrationUploadResponse(
            migrationId = migrationId,
            termProgressCount = termProgressRepository.countByUserId(userId).toInt(),
            glossaryProgressCount = glossaryProgressRepository.countByUserId(userId).toInt(),
            bookmarkCount = bookmarkRepository.countByIdUserId(userId).toInt(),
            dailyProgressCount = dailyProgressRepository.countByIdUserId(userId).toInt()
        )

    private fun scheduleFailureRecordAfterRollback(sessionId: UUID, reason: String) {
        val normalizedReason = reason.ifBlank { DEFAULT_FAILURE_REASON }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            migrationSessionFailureService.markFailed(sessionId, normalizedReason)
            return
        }

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK) return
                runCatching {
                    migrationSessionFailureService.markFailed(sessionId, normalizedReason)
                }.onFailure { throwable ->
                    log.error("migration failure 상태 저장 실패: sessionId={}", sessionId, throwable)
                }
            }
        })
    }

    companion object {
        private const val MIN_EASE_FACTOR = 1.3
        private const val DEFAULT_FAILURE_REASON = "마이그레이션 처리 중 오류가 발생했습니다"
    }
}
