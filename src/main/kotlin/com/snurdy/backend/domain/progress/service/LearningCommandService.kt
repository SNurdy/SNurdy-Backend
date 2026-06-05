package com.snurdy.backend.domain.progress.service

import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.progress.dto.LearningAnswerEvent
import com.snurdy.backend.domain.progress.entity.DailyProgressEntity
import com.snurdy.backend.domain.progress.entity.DailyProgressId
import com.snurdy.backend.domain.progress.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import com.snurdy.backend.domain.progress.entity.TermProgressEntity
import com.snurdy.backend.domain.progress.entity.TermProgressStatus
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.progress.repository.TermProgressRepository
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.domain.user.wrongAnswerNote.service.WrongAnswerNoteService
import com.snurdy.backend.global.constants.UserError
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
    private val wrongAnswerNoteService: WrongAnswerNoteService,
    private val clock: Clock
) {

    @Transactional
    fun submitLearning(userId: UUID, events: List<LearningAnswerEvent>) {
        if (events.isEmpty()) return

        val user = findUser(userId)

        val now = clock.instant()
        val dailyDate = now.atZone(APP_ZONE_ID).toLocalDate()

        val context = prefetchSubmitContext(userId, events)
        val aggregation = LearningAggregation()
        val newProgresses = mutableListOf<TermProgressEntity>()

        events.forEach { event ->
            val progress = context.getOrCreateProgress(user, event, newProgresses)
            val wasFirstLearned = SrsCalculator.applyAnswer(progress, event, now)
            aggregation.accumulate(event.glossaryId, dailyDate, wasFirstLearned)
            if (!event.correct) {
                wrongAnswerNoteService.recordIncorrect(userId, event.termId, event.glossaryId, now)
            }
        }

        if (newProgresses.isNotEmpty()) {
            termProgressRepository.saveAll(newProgresses)
        }
        applyLearningAggregation(user, aggregation, now, dailyDate)
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

        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id }
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds.toList()).associateBy { it.id }
        val progresses = termProgressRepository
            .findAllByUserIdAndGlossaryIdInAndTermIdIn(userId, glossaryIds, termIds)
            .associateByTo(mutableMapOf()) { GlossaryTermId(it.glossary.id, it.term.id) }

        return SubmitContext(validPairs, glossaries, terms, progresses)
    }

    private fun updateGlossaryProgresses(user: UserEntity, learnedByGlossary: Map<Long, Int>, now: Instant) {
        if (learnedByGlossary.isEmpty()) return

        val glossaryIds = learnedByGlossary.keys
        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id }
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
            (todayBefore.learnedCount + todayBefore.completedCount) > 0

        val toSave = byDate.map { (date, delta) ->
            val progress = existing[date] ?: DailyProgressEntity(
                id = DailyProgressId(user.id, date),
                user = user
            )
            progress.learnedCount += delta.learned
            progress.completedCount += delta.completed
            progress
        }
        dailyProgressRepository.saveAll(toSave)
        return todayWasActiveBefore
    }

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
                    ?.let { (it.learnedCount + it.completedCount) > 0 }
                    ?: false
                when {
                    !yesterdayActive -> 1
                    user.currentStreak > 0 -> user.currentStreak + 1
                    // currentStreak 가 stale(0)인데 어제는 활성 → daily_progresses 기반으로 역산
                    else -> countStreakEndingAt(user.id, yesterday) + 1
                }
            }
        }
        user.applyLearningOutcome(learnedDelta = learnedDelta, newCurrentStreak = newStreak)
    }

    private fun countStreakEndingAt(userId: UUID, endDate: LocalDate): Int {
        val rows = dailyProgressRepository.findByIdUserIdAndIdDateLessThanEqualOrderByIdDateDesc(
            userId = userId,
            date = endDate,
            pageable = PageRequest.of(0, MAX_STREAK_LOOKBACK)
        )
        val activeDates = rows.asSequence()
            .filter { (it.learnedCount + it.completedCount) > 0 }
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
        userRepository.findByIdOrNull(userId) ?: throw ApiException(UserError.ACCOUNT_NOT_FOUND)

    companion object {
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private const val MAX_STREAK_LOOKBACK: Int = 365
    }

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

    private data class DailyDelta(val learned: Int, val completed: Int) {
        operator fun plus(other: DailyDelta) = DailyDelta(learned + other.learned, completed + other.completed)

        companion object {
            val EMPTY = DailyDelta(0, 0)
        }
    }

    private class LearningAggregation {
        var learnedTotal: Int = 0
        var completedTotal: Int = 0
        val learnedByGlossary: MutableMap<Long, Int> = mutableMapOf()
        val byDate: MutableMap<LocalDate, DailyDelta> = mutableMapOf()

        fun accumulate(glossaryId: Long, dailyDate: LocalDate, wasFirstLearned: Boolean) {
            completedTotal += 1
            if (wasFirstLearned) {
                learnedTotal += 1
                learnedByGlossary[glossaryId] = (learnedByGlossary[glossaryId] ?: 0) + 1
            }

            val previous = byDate[dailyDate] ?: DailyDelta.EMPTY
            byDate[dailyDate] = previous + DailyDelta(if (wasFirstLearned) 1 else 0, 1)
        }
    }
}
