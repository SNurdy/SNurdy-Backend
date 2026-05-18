package com.snurdy.backend.domain.learning.service

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.dto.LearningAnswerEvent
import com.snurdy.backend.domain.learning.dto.LearningSubmitRequest
import com.snurdy.backend.domain.learning.dto.LearningSubmitResponse
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
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class LearningCommandService(
    private val userRepository: UserRepository,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val clock: Clock
) {

    @Transactional
    fun submitLearning(userId: UUID, request: LearningSubmitRequest): LearningSubmitResponse {
        val user = findUser(userId)
        if (request.events.isEmpty()) {
            return LearningSubmitResponse(processedCount = 0, learnedCount = 0, reviewedCount = 0)
        }

        // 전체 batch 가 같은 시각/날짜를 공유하도록 한 번만 캡처.
        val now = clock.instant()
        val dailyDate = now.atZone(APP_ZONE_ID).toLocalDate()

        val context = prefetchSubmitContext(userId, request.events)
        val aggregation = LearningAggregation()
        val newProgresses = mutableListOf<TermProgressEntity>()

        request.events.forEach { event ->
            val progress = context.getOrCreateProgress(user, event, newProgresses)
            val wasFirstLearned = SrsCalculator.applyAnswer(progress, event, now)
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
     * daily_progresses 를 갱신하고, 오늘 엔트리가 '세션 전부터' 활성이었는지 여부를 돌려준다.
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
                    // currentStreak 가 stale(0)인데 어제는 활성 → daily_progresses 기반으로 역산
                    // 사례: userSummary 없이 마이그레이션한 사용자가 마이그레이션 이후 첫 학습.
                    else -> countStreakEndingAt(user.id, yesterday) + 1
                }
            }
        }
        user.applyLearningOutcome(learnedDelta = learnedDelta, newCurrentStreak = newStreak)
    }

    /**
     * [endDate] 에서 뒤로 연속된 활성일 수를 계산. 성능 보호를 위해 [MAX_STREAK_LOOKBACK] 로 상한 캡.
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

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId) ?: throw ApiException(CommonError.UNAUTHORIZED)

    companion object {
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        /**
         * streak 역산 시 daily_progresses 를 돌아볼 최대 일수.
         * 365일이면 1년 간 매일 활동한 사용자도 수용하며, 단건 쿼리로 끝난다.
         */
        private const val MAX_STREAK_LOOKBACK: Int = 365
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

private val GlossaryEntity.idOrThrow: Long
    get() = id ?: error("GlossaryEntity#id must be non-null after persistence")

private val TermEntity.idOrThrow: Long
    get() = id ?: error("TermEntity#id must be non-null after persistence")
