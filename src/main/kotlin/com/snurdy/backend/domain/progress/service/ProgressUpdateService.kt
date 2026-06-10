package com.snurdy.backend.domain.progress.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.progress.dto.ProgressEvent
import com.snurdy.backend.domain.progress.entity.DailyProgressEntity
import com.snurdy.backend.domain.progress.entity.DailyProgressId
import com.snurdy.backend.domain.progress.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import com.snurdy.backend.domain.progress.entity.TermProgressEntity
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
import com.snurdy.backend.global.constants.APP_ZONE_ID
import java.util.UUID

// 호출자 책임 (TestService 등)
// - events 의 termId 가 단일 glossary 에 속하는 검증을 호출자가 수행할 것
// - 호출자 도메인의 ErrorCode 로 검증 실패를 throw 할 것
// 본 서비스 책임
// - user 존재 확인
// - SRS / GlossaryProgress / DailyProgress / streak / 오답노트 갱신
@Service
class ProgressUpdateService(
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
    fun updateProgress(userId: UUID, events: List<ProgressEvent>) {
        if (events.isEmpty()) return

        val user = findUser(userId)

        val now = clock.instant()
        val dailyDate = now.atZone(APP_ZONE_ID).toLocalDate()

        val context = ProgressBatchCache.prefetch(
            userId = userId,
            events = events,
            glossaryTermRepository = glossaryTermRepository,
            glossaryRepository = glossaryRepository,
            termRepository = termRepository,
            termProgressRepository = termProgressRepository
        )
        val aggregation = ProgressAggregation()
        val newProgresses = mutableListOf<TermProgressEntity>()

        events.forEach { event ->
            val progress = context.getOrCreateProgress(user, event, newProgresses)
            val wasFirstLearned = SrsCalculator.applyAnswer(progress, event, now)
            aggregation.accumulate(event.glossaryId, dailyDate, wasFirstLearned)
        }

        events.filter { !it.correct }
            .groupBy { it.glossaryId }
            .forEach { (glossaryId, group) ->
                wrongAnswerNoteService.recordIncorrects(userId, group.map { it.termId }, glossaryId, now)
            }

        if (newProgresses.isNotEmpty()) {
            termProgressRepository.saveAll(newProgresses)
        }
        applyProgressAggregation(user, aggregation, now, dailyDate)
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
            (todayBefore.newLearnedCount + todayBefore.allLearnedCount) > 0

        val toSave = byDate.map { (date, delta) ->
            val progress = existing[date] ?: DailyProgressEntity(
                id = DailyProgressId(user.id, date),
                user = user
            )
            progress.newLearnedCount += delta.newLearned
            progress.allLearnedCount += delta.allLearned
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
                    ?.let { (it.newLearnedCount + it.allLearnedCount) > 0 }
                    ?: false
                when {
                    !yesterdayActive -> 1
                    user.currentStreak > 0 -> user.currentStreak + 1
                    // currentStreak 가 stale(0)인데 어제는 활성 → daily_progresses 기반으로 역산
                    else -> countStreakEndingAt(user.id, yesterday) + 1
                }
            }
        }
        user.applyProgressOutcome(learnedDelta = learnedDelta, newCurrentStreak = newStreak)
    }

    private fun countStreakEndingAt(userId: UUID, endDate: LocalDate): Int {
        val rows = dailyProgressRepository.findByIdUserIdAndIdDateLessThanEqualOrderByIdDateDesc(
            userId = userId,
            date = endDate,
            pageable = PageRequest.of(0, MAX_STREAK_LOOKBACK)
        )
        val activeDates = rows.asSequence()
            .filter { (it.newLearnedCount + it.allLearnedCount) > 0 }
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

    private fun applyProgressAggregation(
        user: UserEntity,
        aggregation: ProgressAggregation,
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
        private const val MAX_STREAK_LOOKBACK: Int = 365
    }

    private data class DailyDelta(val newLearned: Int, val allLearned: Int) {
        operator fun plus(other: DailyDelta) = DailyDelta(newLearned + other.newLearned, allLearned + other.allLearned)

        companion object {
            val EMPTY = DailyDelta(0, 0)
        }
    }

    private class ProgressAggregation {
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
