package com.snurdy.backend.domain.learning.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.NewLearningCardProjection
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.dto.CalendarDailyItem
import com.snurdy.backend.domain.learning.dto.CalendarStatsResponse
import com.snurdy.backend.domain.learning.dto.LearningTermCard
import com.snurdy.backend.domain.learning.dto.ReviewDueTermsResponse
import com.snurdy.backend.domain.learning.dto.StudyNextTermsResponse
import com.snurdy.backend.domain.learning.dto.UserStreakResponse
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.LearningError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@Service
class LearningQueryService(
    private val userRepository: UserRepository,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termProgressRepository: TermProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val clock: Clock
) {

    private fun today(): LocalDate = LocalDate.now(clock.withZone(APP_ZONE_ID))

    @Transactional(readOnly = true)
    fun getNextTerms(userId: UUID, glossaryId: Long, size: Int): StudyNextTermsResponse {
        val glossary = findGlossary(glossaryId)
        val glossaryKey = glossary.idOrThrow
        val safeSize = size.coerceIn(1, 100)

        val cards = glossaryTermRepository
            .findNewLearningCards(userId, glossaryKey, safeSize)
            .map { toNewLearningCard(it) }

        return StudyNextTermsResponse(glossaryId = glossaryKey, terms = cards)
    }

    @Transactional(readOnly = true)
    fun getDueTerms(userId: UUID, glossaryId: Long, size: Int): ReviewDueTermsResponse {
        val glossary = findGlossary(glossaryId)
        val glossaryKey = glossary.idOrThrow
        val safeSize = size.coerceIn(1, 100)
        val now = clock.instant()

        val dueProgresses = termProgressRepository.findDueProgresses(
            userId = userId,
            glossaryId = glossaryKey,
            now = now,
            statuses = listOf(TermProgressStatus.LEARNING, TermProgressStatus.REVIEW),
            pageable = PageRequest.of(0, safeSize)
        )

        val termIds = dueProgresses.mapNotNull { it.term.id }
        val termMap = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.idOrThrow }
        val bookmarkedTermIds = if (termIds.isEmpty()) {
            emptySet()
        } else {
            bookmarkRepository.findAllByIdUserIdAndIdTermIdIn(userId, termIds)
                .map { it.id.termId }
                .toSet()
        }

        val cards = dueProgresses.mapNotNull { progress ->
            val termId = progress.term.id ?: return@mapNotNull null
            val term = termMap[termId] ?: return@mapNotNull null
            toLearningCard(progress, term, termId in bookmarkedTermIds)
        }

        return ReviewDueTermsResponse(glossaryId = glossaryKey, terms = cards)
    }

    @Transactional(readOnly = true)
    fun getCalendar(userId: UUID, year: Int, month: Int): CalendarStatsResponse {
        if (year !in VALID_YEAR_RANGE) throw ApiException(LearningError.INVALID_YEAR_RANGE)
        if (month !in 1..12) throw ApiException(LearningError.INVALID_MONTH_RANGE)

        val yearMonth = YearMonth.of(year, month)
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()

        val progresses = dailyProgressRepository.findByIdUserIdAndIdDateBetweenOrderByIdDateAsc(userId, from, to)
        val days = progresses.map {
            CalendarDailyItem(
                date = it.id.date,
                learnedCount = it.learnedCount,
                reviewedCount = it.reviewedCount
            )
        }

        return CalendarStatsResponse(
            year = year,
            month = month,
            days = days,
            totalLearnedCount = days.sumOf { it.learnedCount },
            totalReviewedCount = days.sumOf { it.reviewedCount }
        )
    }

    @Transactional(readOnly = true)
    fun getStreak(userId: UUID): UserStreakResponse {
        val user = findUser(userId)
        val effectiveCurrent = if (isStreakStillAlive(userId, user.currentStreak)) {
            user.currentStreak
        } else {
            0
        }
        return UserStreakResponse(
            currentStreak = effectiveCurrent,
            longestStreak = user.longestStreak,
            totalLearningTerms = user.totalLearningTerms
        )
    }

    /**
     * 저장된 currentStreak 가 현 시점에서도 유효한지 판정.
     * - 저장값이 0 이면 비교할 필요 없음
     * - 오늘 또는 어제 활성이면 streak 유지 (자정 전까지는 어제 활성도 회복 기회로 인정)
     * - 그 외(그제 이전이 마지막 활성)면 만료
     *
     * DB는 갱신하지 않는다(read-only). 다음 학습 제출 시 정상 경로로 재계산된다.
     */
    private fun isStreakStillAlive(userId: UUID, storedStreak: Int): Boolean {
        if (storedStreak == 0) return true
        val today = today()
        val yesterday = today.minusDays(1)
        val recent = dailyProgressRepository
            .findAllByIdUserIdAndIdDateIn(userId, listOf(today, yesterday))
        return recent.any { (it.learnedCount + it.reviewedCount) > 0 }
    }

    private fun toNewLearningCard(projection: NewLearningCardProjection): LearningTermCard =
        LearningTermCard(
            termId = projection.termId,
            spelling = projection.spelling,
            meaning = projection.meaning,
            abbreviation = projection.abbreviation,
            status = TermProgressStatus.NEW.name,
            easeFactor = SrsCalculator.DEFAULT_EASE_FACTOR,
            intervalDays = 0,
            reviewCount = 0,
            lastReviewedAt = null,
            nextReviewAt = null,
            bookmarked = projection.bookmarked == 1
        )

    private fun toLearningCard(progress: TermProgressEntity, term: TermEntity, bookmarked: Boolean): LearningTermCard =
        LearningTermCard(
            termId = term.idOrThrow,
            spelling = term.spelling,
            meaning = term.meaning,
            abbreviation = term.abbreviation,
            status = progress.status.name,
            easeFactor = progress.easeFactor,
            intervalDays = progress.intervalDays,
            reviewCount = progress.reviewCount,
            lastReviewedAt = progress.lastReviewedAt,
            nextReviewAt = progress.nextReviewAt,
            bookmarked = bookmarked
        )

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId) ?: throw ApiException(CommonError.UNAUTHORIZED)

    private fun findGlossary(glossaryId: Long): GlossaryEntity =
        glossaryRepository.findByIdOrNull(glossaryId)
            ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)

    companion object {
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val VALID_YEAR_RANGE: IntRange = 2000..2100
    }
}

private val GlossaryEntity.idOrThrow: Long
    get() = id ?: error("GlossaryEntity#id must be non-null after persistence")

private val TermEntity.idOrThrow: Long
    get() = id ?: error("TermEntity#id must be non-null after persistence")
