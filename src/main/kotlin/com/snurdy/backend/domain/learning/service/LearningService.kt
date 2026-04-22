package com.snurdy.backend.domain.learning.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.dto.CalendarDailyItem
import com.snurdy.backend.domain.learning.dto.CalendarStatsResponse
import com.snurdy.backend.domain.learning.dto.LearningAnswerEvent
import com.snurdy.backend.domain.learning.dto.LearningSubmitRequest
import com.snurdy.backend.domain.learning.dto.LearningSubmitResponse
import com.snurdy.backend.domain.learning.dto.LearningTermCard
import com.snurdy.backend.domain.learning.dto.ReviewDueTermsResponse
import com.snurdy.backend.domain.learning.dto.StudyNextTermsResponse
import com.snurdy.backend.domain.learning.dto.UserStreakResponse
import com.snurdy.backend.domain.learning.entity.DailyProgressEntity
import com.snurdy.backend.domain.learning.entity.DailyProgressId
import com.snurdy.backend.domain.learning.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.learning.entity.GlossaryProgressId
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil

@Service
class LearningService(
    private val userRepository: UserRepository,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val bookmarkRepository: BookmarkRepository
) {

    @Transactional(readOnly = true)
    fun getNextTerms(userId: UUID, glossaryId: Long, size: Int): StudyNextTermsResponse {
        val glossary = findGlossary(glossaryId)
        val glossaryKey = glossary.id!!
        val safeSize = size.coerceIn(1, 100)

        val termSummaries = glossaryTermRepository.findTermSummariesByGlossaryId(glossaryKey)
        if (termSummaries.isEmpty()) return StudyNextTermsResponse(glossaryId = glossaryKey, terms = emptyList())

        val termIds = termSummaries.map { it.termId }
        val progressMap = termProgressRepository.findByUserIdAndGlossaryIdAndTermIdIn(userId, glossaryKey, termIds)
            .associateBy { it.term.id!! }
        val bookmarkedTermIds = bookmarkRepository.findAllByIdUserIdAndIdTermIdIn(userId, termIds)
            .map { it.id.termId }
            .toSet()

        val cards = termSummaries
            .asSequence()
            .mapNotNull { summary ->
                val progress = progressMap[summary.termId]
                val status = progress?.status ?: TermProgressStatus.NEW
                if (status != TermProgressStatus.NEW) return@mapNotNull null

                LearningTermCard(
                    termId = summary.termId,
                    spelling = summary.spelling,
                    meaning = summary.meaning,
                    abbreviation = summary.abbreviation,
                    status = status.name,
                    easeFactor = progress?.easeFactor ?: DEFAULT_EASE_FACTOR,
                    intervalDays = progress?.intervalDays ?: 0,
                    reviewCount = progress?.reviewCount ?: 0,
                    lastReviewedAt = progress?.lastReviewedAt,
                    nextReviewAt = progress?.nextReviewAt,
                    bookmarked = summary.termId in bookmarkedTermIds
                )
            }
            .take(safeSize)
            .toList()

        return StudyNextTermsResponse(glossaryId = glossaryKey, terms = cards)
    }

    @Transactional(readOnly = true)
    fun getDueTerms(userId: UUID, glossaryId: Long, size: Int): ReviewDueTermsResponse {
        val glossary = findGlossary(glossaryId)
        val glossaryKey = glossary.id!!
        val safeSize = size.coerceIn(1, 100)
        val now = Instant.now()

        val dueProgresses = termProgressRepository.findDueProgresses(
            userId = userId,
            glossaryId = glossaryKey,
            now = now,
            statuses = listOf(TermProgressStatus.LEARNING, TermProgressStatus.REVIEW),
            pageable = PageRequest.of(0, safeSize)
        )

        val termIds = dueProgresses.mapNotNull { it.term.id }
        val termMap = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id!! }
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

    @Transactional
    fun submitLearning(userId: UUID, request: LearningSubmitRequest): LearningSubmitResponse {
        val user = findUser(userId)
        val aggregation = LearningAggregation()

        request.events.forEach { event ->
            processLearningEvent(user, event, aggregation)
        }
        applyLearningAggregation(user, aggregation)

        return LearningSubmitResponse(
            processedCount = request.events.size,
            learnedCount = aggregation.learnedTotal,
            reviewedCount = aggregation.reviewedTotal
        )
    }

    @Transactional(readOnly = true)
    fun getCalendar(userId: UUID, year: Int, month: Int): CalendarStatsResponse {
        if (month !in 1..12) throw ApiException(CommonError.BAD_REQUEST, "month는 1..12 범위여야 합니다")

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
        return UserStreakResponse(
            currentStreak = user.currentStreak,
            longestStreak = user.longestStreak,
            totalLearningTerms = user.totalLearningTerms
        )
    }

    private fun findOrCreateProgress(user: UserEntity, event: LearningAnswerEvent): TermProgressEntity {
        validateGlossaryTermPair(event.glossaryId, event.termId)
        val glossary = findGlossary(event.glossaryId)
        val term = findTerm(event.termId)

        return termProgressRepository.findByUserIdAndGlossaryIdAndTermId(user.id, glossary.id!!, term.id!!)
            ?: termProgressRepository.save(
                TermProgressEntity(
                    user = user,
                    glossary = glossary,
                    term = term,
                    status = TermProgressStatus.NEW
                )
            )
    }

    private fun applyAnswer(progress: TermProgressEntity, event: LearningAnswerEvent, now: Instant): Boolean {
        if (event.correct) {
            val previousReviewCount = progress.reviewCount
            val nextReviewCount = previousReviewCount + 1
            val nextInterval = calculateInterval(progress.intervalDays, progress.easeFactor, nextReviewCount)

            progress.reviewCount = nextReviewCount
            progress.intervalDays = nextInterval
            progress.status = if (nextReviewCount >= 3) TermProgressStatus.REVIEW else TermProgressStatus.LEARNING
            progress.lastReviewedAt = now
            progress.nextReviewAt = now.plus(nextInterval.toLong(), ChronoUnit.DAYS)

            return previousReviewCount == 0
        }

        progress.easeFactor = progress.easeFactor
            .subtract(EASE_DECREASE)
            .max(MIN_EASE_FACTOR)
            .setScale(2, RoundingMode.HALF_UP)
        progress.intervalDays = 1
        progress.reviewCount = 0
        progress.status = TermProgressStatus.LEARNING
        progress.lastReviewedAt = now
        progress.nextReviewAt = now.plus(1, ChronoUnit.DAYS)
        return false
    }

    private fun calculateInterval(currentInterval: Int, easeFactor: BigDecimal, reviewCount: Int): Int =
        when (reviewCount) {
            1 -> 1
            2 -> 3
            else -> ceil(currentInterval.coerceAtLeast(1) * easeFactor.toDouble()).toInt().coerceAtLeast(1)
        }

    private fun updateGlossaryProgresses(user: UserEntity, learnedByGlossary: Map<Long, Int>) {
        if (learnedByGlossary.isEmpty()) return

        learnedByGlossary.forEach { (glossaryId, learnedDelta) ->
            val glossary = findGlossary(glossaryId)
            val progress = glossaryProgressRepository.findByIdUserIdAndIdGlossaryId(user.id, glossary.id!!)
                ?: GlossaryProgressEntity(
                    id = GlossaryProgressId(user.id, glossary.id),
                    user = user,
                    glossary = glossary
                )

            progress.learnedCount += learnedDelta
            progress.lastLearnedAt = Instant.now()
            glossaryProgressRepository.save(progress)
        }
    }

    private fun updateDailyProgresses(user: UserEntity, byDate: Map<LocalDate, DailyDelta>) {
        if (byDate.isEmpty()) return

        byDate.forEach { (date, delta) ->
            val progress = dailyProgressRepository.findByIdOrNull(DailyProgressId(user.id, date))
                ?: DailyProgressEntity(
                    id = DailyProgressId(user.id, date),
                    user = user
                )

            progress.learnedCount += delta.learned
            progress.reviewedCount += delta.reviewed
            dailyProgressRepository.save(progress)
        }
    }

    private fun updateUserSummary(user: UserEntity, learnedDelta: Int) {
        if (learnedDelta > 0) {
            user.totalLearningTerms += learnedDelta
        }

        val activeDates = dailyProgressRepository.findByIdUserIdOrderByIdDateDesc(user.id)
            .mapNotNull { if (it.learnedCount + it.reviewedCount > 0) it.id.date else null }
            .toSet()

        val streak = calculateCurrentStreak(activeDates)

        user.currentStreak = streak
        user.longestStreak = maxOf(user.longestStreak, streak)
    }

    private fun processLearningEvent(
        user: UserEntity,
        event: LearningAnswerEvent,
        aggregation: LearningAggregation
    ) {
        val now = Instant.now()
        val dailyDate = now.atZone(APP_ZONE_ID).toLocalDate()

        val progress = findOrCreateProgress(user, event)
        val wasFirstLearned = applyAnswer(progress, event, now)
        aggregation.accumulate(event.glossaryId, dailyDate, wasFirstLearned)
    }

    private fun applyLearningAggregation(user: UserEntity, aggregation: LearningAggregation) {
        updateGlossaryProgresses(user, aggregation.learnedByGlossary)
        updateDailyProgresses(user, aggregation.byDate)
        updateUserSummary(user, aggregation.learnedTotal)
    }

    private fun calculateCurrentStreak(activeDates: Set<LocalDate>): Int {
        var streak = 0
        var cursor = LocalDate.now(APP_ZONE_ID)
        while (activeDates.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun toLearningCard(progress: TermProgressEntity, term: TermEntity, bookmarked: Boolean): LearningTermCard =
        LearningTermCard(
            termId = term.id!!,
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

    private fun validateGlossaryTermPair(glossaryId: Long, termId: Long) {
        if (!glossaryTermRepository.existsByIdGlossaryIdAndIdTermId(glossaryId, termId)) {
            throw ApiException(CommonError.BAD_REQUEST, "용어장과 용어의 조합이 유효하지 않습니다")
        }
    }

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId) ?: throw ApiException(CommonError.UNAUTHORIZED)

    private fun findGlossary(glossaryId: Long): GlossaryEntity =
        glossaryRepository.findByIdOrNull(glossaryId)
            ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)

    private fun findTerm(termId: Long): TermEntity =
        termRepository.findByIdAndDeletedAtIsNull(termId)
            .orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }

    companion object {
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DEFAULT_EASE_FACTOR: BigDecimal = BigDecimal("2.50")
        private val MIN_EASE_FACTOR: BigDecimal = BigDecimal("1.30")
        private val EASE_DECREASE: BigDecimal = BigDecimal("0.20")
    }

    private data class DailyDelta(val learned: Int, val reviewed: Int) {
        operator fun plus(other: DailyDelta) = DailyDelta(learned + other.learned, reviewed + other.reviewed)

        companion object {
            val EMPTY = DailyDelta(0, 0)
        }
    }

    private class LearningAggregation {
        var learnedTotal: Int = 0
        var reviewedTotal: Int = 0
        val learnedByGlossary: MutableMap<Long, Int> = mutableMapOf()
        val byDate: MutableMap<LocalDate, DailyDelta> = mutableMapOf()

        fun accumulate(glossaryId: Long, dailyDate: LocalDate, wasFirstLearned: Boolean) {
            reviewedTotal += 1
            if (wasFirstLearned) {
                learnedTotal += 1
                learnedByGlossary[glossaryId] = (learnedByGlossary[glossaryId] ?: 0) + 1
            }

            val previous = byDate[dailyDate] ?: DailyDelta.EMPTY
            byDate[dailyDate] = previous + DailyDelta(if (wasFirstLearned) 1 else 0, 1)
        }
    }
}
