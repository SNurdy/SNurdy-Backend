package com.snurdy.backend.domain.migration.service

import com.snurdy.backend.domain.bookmark.entity.BookmarkEntity
import com.snurdy.backend.domain.bookmark.entity.BookmarkId
import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.progress.entity.DailyProgressEntity
import com.snurdy.backend.domain.progress.entity.DailyProgressId
import com.snurdy.backend.domain.progress.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import com.snurdy.backend.domain.progress.entity.TermProgressEntity
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.progress.repository.TermProgressRepository
import com.snurdy.backend.domain.progress.service.SrsCalculator
import com.snurdy.backend.domain.migration.dto.BookmarkPayload
import com.snurdy.backend.domain.migration.dto.DailyProgressPayload
import com.snurdy.backend.domain.migration.dto.GlossaryProgressPayload
import com.snurdy.backend.domain.migration.dto.MigrationUploadRequest
import com.snurdy.backend.domain.migration.dto.MigrationUploadResponse
import com.snurdy.backend.domain.migration.dto.TermProgressPayload
import com.snurdy.backend.domain.migration.dto.UserSummaryPayload
import com.snurdy.backend.domain.migration.repository.MigrationSessionRepository
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.global.constants.MigrationError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * 마이그레이션 페이로드 검증과 일괄 저장을 책임진다.
 * 세션 라이프사이클/실패 기록은 MigrationSessionService 에 위임한다.
 */
@Service
class MigrationUploadService(
    private val migrationSessionService: MigrationSessionService,
    private val migrationSessionRepository: MigrationSessionRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val termRepository: TermRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val glossaryRepository: GlossaryRepository,
    private val clock: Clock
) {

    private fun now(): Instant = clock.instant()

    @Transactional
    fun upload(userId: UUID, migrationId: String, request: MigrationUploadRequest): MigrationUploadResponse {
        val user = migrationSessionService.findUser(userId)
        if (user.migrationDeclinedAt != null) {
            throw ApiException(MigrationError.MIGRATION_DECLINED)
        }

        validateDuplicateEntries(request)

        val session = migrationSessionService.findSessionForUpdate(migrationId)
        if (session.user.id != userId) throw ApiException(MigrationError.MIGRATION_SESSION_FORBIDDEN)

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
            migrationSessionService.scheduleFailureRecord(session.id, e.responseMessage)
            throw e
        } catch (e: Exception) {
            migrationSessionService.scheduleFailureRecord(session.id, MigrationError.MIGRATION_UPLOAD_INTERNAL_ERROR.message)
            throw e
        }
    }

    private fun saveTermProgresses(user: UserEntity, payloads: List<TermProgressPayload>): Int {
        if (payloads.isEmpty()) return 0

        val glossaryIds = payloads.map { it.glossaryId }.distinct()
        val termIds = payloads.map { it.termId }.distinct()

        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id }
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id }

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

        val migratedAt = now()
        val entities = payloads.map { payload ->
            val glossary = glossaries[payload.glossaryId]
                ?: throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)
            val term = terms[payload.termId]
                ?: throw ApiException(MigrationError.MIGRATION_INVALID_TERM_ID)
            val reviewCount = payload.reviewCount.coerceAtLeast(0)
            val intervalDays = payload.intervalDays.coerceAtLeast(0)
            val lastReviewedAt = payload.lastReviewedAt ?: if (reviewCount > 0) migratedAt else null

            TermProgressEntity(
                user = user,
                glossary = glossary,
                term = term,
                status = payload.status,
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
        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id }

        val missingGlossaryIds = glossaryIds.filterNot(glossaries::containsKey)
        if (missingGlossaryIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)
        }

        val entities = payloads.map { payload ->
            val glossary = glossaries[payload.glossaryId]
                ?: throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)

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

        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id }
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id }

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
            val glossary = glossaries[payload.glossaryId]
                ?: throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)
            val term = terms[payload.termId]
                ?: throw ApiException(MigrationError.MIGRATION_INVALID_TERM_ID)
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
                completedCount = payload.reviewedCount.coerceAtLeast(0)
            )
        }
        dailyProgressRepository.saveAll(entities)
        return entities.size
    }

    private fun applyUserSummary(user: UserEntity, payload: UserSummaryPayload?) {
        if (payload == null) return
        user.replaceSummaryFromMigration(
            currentStreakValue = payload.currentStreak,
            longestStreakValue = payload.longestStreak,
            totalLearning = payload.totalLearningTerms
        )
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
        rejectIfDuplicated(request.termProgresses, MigrationError.MIGRATION_DUPLICATED_TERM_PROGRESS) {
            it.glossaryId to it.termId
        }
        rejectIfDuplicated(request.glossaryProgresses, MigrationError.MIGRATION_DUPLICATED_GLOSSARY_PROGRESS) {
            it.glossaryId
        }
        rejectIfDuplicated(request.bookmarks, MigrationError.MIGRATION_DUPLICATED_BOOKMARK) {
            it.glossaryId to it.termId
        }
        rejectIfDuplicated(request.dailyProgresses, MigrationError.MIGRATION_DUPLICATED_DAILY_PROGRESS) {
            it.date
        }
    }

    private inline fun <T, K> rejectIfDuplicated(
        items: List<T>,
        error: MigrationError,
        crossinline keySelector: (T) -> K
    ) {
        val duplicated = items.groupingBy(keySelector).eachCount().filterValues { it > 1 }
        if (duplicated.isNotEmpty()) throw ApiException(error)
    }

    private fun normalizeEaseFactor(value: Double): BigDecimal =
        value
            .coerceAtLeast(SrsCalculator.MIN_EASE_FACTOR.toDouble())
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
}
