package com.snurdy.backend.domain.learning.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.NewLearningCardProjection
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
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.LearningError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
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
    private val bookmarkRepository: BookmarkRepository,
    private val clock: Clock
) {

    private fun now(): Instant = clock.instant()
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

    private fun toNewLearningCard(projection: NewLearningCardProjection): LearningTermCard =
        LearningTermCard(
            termId = projection.termId,
            spelling = projection.spelling,
            meaning = projection.meaning,
            abbreviation = projection.abbreviation,
            status = TermProgressStatus.NEW.name,
            easeFactor = DEFAULT_EASE_FACTOR,
            intervalDays = 0,
            reviewCount = 0,
            lastReviewedAt = null,
            nextReviewAt = null,
            bookmarked = projection.bookmarked == 1
        )

    @Transactional(readOnly = true)
    fun getDueTerms(userId: UUID, glossaryId: Long, size: Int): ReviewDueTermsResponse {
        val glossary = findGlossary(glossaryId)
        val glossaryKey = glossary.idOrThrow
        val safeSize = size.coerceIn(1, 100)
        val now = now()

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

    @Transactional
    fun submitLearning(userId: UUID, request: LearningSubmitRequest): LearningSubmitResponse {
        val user = findUser(userId)
        if (request.events.isEmpty()) {
            return LearningSubmitResponse(processedCount = 0, learnedCount = 0, reviewedCount = 0)
        }

        // 전체 batch가 같은 시각/날짜를 공유하도록 한 번만 캡처.
        val now = now()
        val dailyDate = now.atZone(APP_ZONE_ID).toLocalDate()

        val context = prefetchSubmitContext(userId, request.events)
        val aggregation = LearningAggregation()
        val newProgresses = mutableListOf<TermProgressEntity>()

        request.events.forEach { event ->
            val progress = context.getOrCreateProgress(user, event, newProgresses)
            val wasFirstLearned = applyAnswer(progress, event, now)
            aggregation.accumulate(event.glossaryId, dailyDate, wasFirstLearned)
        }

        if (newProgresses.isNotEmpty()) {
            termProgressRepository.saveAll(newProgresses)
        }
        applyLearningAggregation(user, aggregation, now, dailyDate)

        return LearningSubmitResponse(
            processedCount = request.events.size,
            learnedCount = aggregation.learnedTotal,
            reviewedCount = aggregation.reviewedTotal
        )
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
     * DB는 갱신하지 않는다(read-only). 다음 학습 제출 시 updateUserSummary 에서 정상 경로로 재계산된다.
     */
    private fun isStreakStillAlive(userId: UUID, storedStreak: Int): Boolean {
        if (storedStreak == 0) return true
        val today = today()
        val yesterday = today.minusDays(1)
        val recent = dailyProgressRepository
            .findAllByIdUserIdAndIdDateIn(userId, listOf(today, yesterday))
        return recent.any { (it.learnedCount + it.reviewedCount) > 0 }
    }

    /**
     * submitLearning 진입 시점에 batch 전체에서 필요한 엔티티를 일괄 조회해 캐시한다.
     * 이벤트 순회 중에는 DB hit 없이 메모리 lookup만 수행.
     */
    private fun prefetchSubmitContext(userId: UUID, events: List<LearningAnswerEvent>): SubmitContext {
        val glossaryIds = events.mapTo(mutableSetOf()) { it.glossaryId }
        val termIds = events.mapTo(mutableSetOf()) { it.termId }

        val validPairs = glossaryTermRepository
            .findAllByIdGlossaryIdInAndIdTermIdIn(glossaryIds, termIds)
            .mapTo(mutableSetOf()) { it.id }

        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.idOrThrow }
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds.toList()).associateBy { it.idOrThrow }
        val progresses = termProgressRepository
            .findAllByUserIdAndGlossaryIdInAndTermIdIn(userId, glossaryIds, termIds)
            .associateByTo(mutableMapOf()) { GlossaryTermId(it.glossary.idOrThrow, it.term.idOrThrow) }

        return SubmitContext(validPairs, glossaries, terms, progresses)
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

    private fun updateGlossaryProgresses(user: UserEntity, learnedByGlossary: Map<Long, Int>, now: Instant) {
        if (learnedByGlossary.isEmpty()) return

        val glossaryIds = learnedByGlossary.keys
        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.idOrThrow }
        val existing = glossaryProgressRepository
            .findAllByIdUserIdAndIdGlossaryIdIn(user.id, glossaryIds)
            .associateBy { it.id.glossaryId }

        val toSave = learnedByGlossary.map { (glossaryId, learnedDelta) ->
            val progress = existing[glossaryId] ?: GlossaryProgressEntity(
                id = GlossaryProgressId(user.id, glossaryId),
                user = user,
                glossary = glossaries[glossaryId] ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
            )
            progress.learnedCount += learnedDelta
            progress.lastLearnedAt = now
            progress
        }
        glossaryProgressRepository.saveAll(toSave)
    }

    /**
     * daily_progresses를 갱신하고, 오늘 엔트리가 '세션 전부터' 활성이었는지 여부를 돌려준다.
     * streak 증분 계산에 필요한 유일한 기존 상태 신호.
     */
    private fun updateDailyProgresses(
        user: UserEntity,
        byDate: Map<LocalDate, DailyDelta>,
        today: LocalDate
    ): Boolean {
        if (byDate.isEmpty()) return false

        val existing = dailyProgressRepository
            .findAllByIdUserIdAndIdDateIn(user.id, byDate.keys)
            .associateBy { it.id.date }

        val todayBefore = existing[today]
        val todayWasActiveBefore = todayBefore != null &&
            (todayBefore.learnedCount + todayBefore.reviewedCount) > 0

        val toSave = byDate.map { (date, delta) ->
            val progress = existing[date] ?: DailyProgressEntity(
                id = DailyProgressId(user.id, date),
                user = user
            )
            progress.learnedCount += delta.learned
            progress.reviewedCount += delta.reviewed
            progress
        }
        dailyProgressRepository.saveAll(toSave)
        return todayWasActiveBefore
    }

    /**
     * streak 증분 계산 — 전체 daily 스캔 대신 오늘/어제 2건만 조회.
     *   - 오늘이 세션 전부터 이미 활성이었다면 streak는 이미 오늘을 포함하므로 변경 없음
     *   - 오늘이 신규 활성이면 어제가 활성이었는지로 +1 vs 1 결정
     *   - 오늘에 학습이 없었다면 streak는 과거 값 유지(만료는 별도 배치 또는 조회 시점에서 해결)
     */
    private fun updateUserSummary(
        user: UserEntity,
        learnedDelta: Int,
        todayWasActiveBefore: Boolean,
        today: LocalDate
    ) {
        val newStreak = when {
            // 오늘 세션 이전부터 이미 활성 → 현재 streak 에 이미 오늘이 포함됨. 단 stale=0인 엣지케이스 방어
            todayWasActiveBefore -> if (user.currentStreak > 0) user.currentStreak else countStreakEndingAt(user.id, today)
            else -> {
                val yesterday = today.minusDays(1)
                val yesterdayActive = dailyProgressRepository
                    .findByIdOrNull(DailyProgressId(user.id, yesterday))
                    ?.let { (it.learnedCount + it.reviewedCount) > 0 }
                    ?: false
                when {
                    !yesterdayActive -> 1
                    user.currentStreak > 0 -> user.currentStreak + 1
                    // currentStreak가 stale(0)인데 어제는 활성 → daily_progresses 기반으로 역산
                    // 사례: userSummary 없이 마이그레이션한 사용자가 마이그레이션 이후 첫 학습.
                    else -> countStreakEndingAt(user.id, yesterday) + 1
                }
            }
        }
        user.applyLearningOutcome(learnedDelta = learnedDelta, newCurrentStreak = newStreak)
    }

    /**
     * [endDate]에서 뒤로 연속된 활성일 수를 계산. 성능 보호를 위해 [MAX_STREAK_LOOKBACK]로 상한 캡.
     * 이 경로는 streak stale 감지/마이그레이션 직후 등 드문 케이스에서만 호출된다.
     */
    private fun countStreakEndingAt(userId: UUID, endDate: LocalDate): Int {
        val rows = dailyProgressRepository.findByIdUserIdAndIdDateLessThanEqualOrderByIdDateDesc(
            userId = userId,
            date = endDate,
            pageable = PageRequest.of(0, MAX_STREAK_LOOKBACK)
        )
        val activeDates = rows.asSequence()
            .filter { (it.learnedCount + it.reviewedCount) > 0 }
            .map { it.id.date }
            .toHashSet()

        var streak = 0
        var cursor = endDate
        while (streak < MAX_STREAK_LOOKBACK && cursor in activeDates) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun applyLearningAggregation(
        user: UserEntity,
        aggregation: LearningAggregation,
        now: Instant,
        today: LocalDate
    ) {
        updateGlossaryProgresses(user, aggregation.learnedByGlossary, now)
        val todayWasActiveBefore = updateDailyProgresses(user, aggregation.byDate, today)
        updateUserSummary(user, aggregation.learnedTotal, todayWasActiveBefore, today)
    }

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
        private val DEFAULT_EASE_FACTOR: BigDecimal = BigDecimal("2.50")
        /**
         * streak 역산 시 daily_progresses 를 돌아볼 최대 일수.
         * 365일이면 1년 간 매일 활동한 사용자도 수용하며, 단건 쿼리로 끝난다.
         */
        private const val MAX_STREAK_LOOKBACK: Int = 365
        private val MIN_EASE_FACTOR: BigDecimal = BigDecimal("1.30")
        private val EASE_DECREASE: BigDecimal = BigDecimal("0.20")
        private val VALID_YEAR_RANGE: IntRange = 2000..2100
    }

    /**
     * submitLearning 한 번의 호출 동안 필요한 엔티티들을 미리 로드해 두는 in-memory 캐시.
     * - validPairs: glossary-term 매핑 검증용
     * - glossaries / terms: 신규 progress 생성 시 참조용
     * - progresses: 기존 progress 캐시 + 신규 생성된 progress 도 같은 batch 내 재사용 위해 추가
     */
    private class SubmitContext(
        private val validPairs: Set<GlossaryTermId>,
        private val glossaries: Map<Long, GlossaryEntity>,
        private val terms: Map<Long, TermEntity>,
        private val progresses: MutableMap<GlossaryTermId, TermProgressEntity>
    ) {
        fun getOrCreateProgress(
            user: UserEntity,
            event: LearningAnswerEvent,
            newProgresses: MutableList<TermProgressEntity>
        ): TermProgressEntity {
            val key = GlossaryTermId(event.glossaryId, event.termId)
            if (key !in validPairs) throw ApiException(DictionaryError.TERM_NOT_IN_GLOSSARY)

            progresses[key]?.let { return it }

            val glossary = glossaries[event.glossaryId]
                ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
            val term = terms[event.termId]
                ?: throw ApiException(DictionaryError.TERM_NOT_FOUND)

            val newProgress = TermProgressEntity(
                user = user,
                glossary = glossary,
                term = term,
                status = TermProgressStatus.NEW
            )
            progresses[key] = newProgress
            newProgresses += newProgress
            return newProgress
        }
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

/**
 * JPA persistent 상태 보장이 깨졌을 때(영속 전 인스턴스를 잘못 다룬 경우) 빠르게 실패시키기 위한 헬퍼.
 * 도메인 침해 없이 호출처에서 `entity.id!!` 대신 `entity.idOrThrow` 를 쓰면 의도가 드러나고 NPE 메시지가 명확해진다.
 */
private val GlossaryEntity.idOrThrow: Long
    get() = id ?: error("GlossaryEntity#id must be non-null after persistence")

private val TermEntity.idOrThrow: Long
    get() = id ?: error("TermEntity#id must be non-null after persistence")
