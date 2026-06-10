package com.snurdy.backend.domain.dashboard.study.service

import com.snurdy.backend.domain.dashboard.study.dto.StudyDailyStatistic
import com.snurdy.backend.domain.dashboard.study.dto.StudyDashboardResponse
import com.snurdy.backend.domain.dashboard.study.dto.StudyFrequentlyIncorrectTerm
import com.snurdy.backend.domain.dashboard.study.dto.StudySelectedGlossary
import com.snurdy.backend.domain.dashboard.study.dto.StudyWeekStatistics
import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.user.preference.service.UserPreferenceService
import com.snurdy.backend.domain.user.streak.service.UserStreakService
import com.snurdy.backend.domain.user.wrongAnswerNote.repository.WrongAnswerTermRepository
import com.snurdy.backend.global.constants.APP_ZONE_ID
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

// 학습하기 탭 대시보드 BFF
// streak, 선택 단어장 다마고치, 한 세션 카드 수, 이번 주 학습 여부, 자주 틀린 단어를 한 응답으로 합성
@Service
class StudyDashboardService(
    private val userStreakService: UserStreakService,
    private val userPreferenceService: UserPreferenceService,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val wrongAnswerTermRepository: WrongAnswerTermRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getDashboard(userId: UUID): StudyDashboardResponse {
        val streak = userStreakService.getStreak(userId)
        val preference = userPreferenceService.getPreference(userId)
        val selectedGlossary = buildSelectedGlossary(userId, preference.selectedGlossaryId)
        val weekStatistics = buildWeekStatistics(userId)
        // 자주 틀린 단어는 사용자가 가장 최근 학습한 단어장 기준 (selectedGlossaryId 와 별개)
        val recentGlossaryId = glossaryProgressRepository.findAllByIdUserId(userId)
            .filter { it.lastLearnedAt != null }
            .maxByOrNull { it.lastLearnedAt!! }
            ?.id?.glossaryId
        val frequentlyIncorrectTerm = recentGlossaryId?.let { glossaryId ->
            wrongAnswerTermRepository
                .findRandomFrequentlyIncorrect(userId, glossaryId, FREQUENTLY_INCORRECT_MIN_COUNT)
                ?.let {
                    StudyFrequentlyIncorrectTerm(
                        termId = it.termId,
                        spelling = it.spelling,
                        meaning = it.meaning,
                        abbreviation = it.abbreviation
                    )
                }
        }

        return StudyDashboardResponse(
            currentStreak = streak.currentStreak,
            selectedGlossary = selectedGlossary,
            defaultCardCount = preference.defaultCardCount,
            weekStatistics = weekStatistics,
            frequentlyIncorrectTerm = frequentlyIncorrectTerm
        )
    }

    private fun buildSelectedGlossary(userId: UUID, selectedGlossaryId: Long): StudySelectedGlossary {
        val meta = glossaryRepository.findGlossaryTermCounts()
            .firstOrNull { it.glossaryId == selectedGlossaryId }
            ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)

        val progress = glossaryProgressRepository.findAllByIdUserId(userId)
            .firstOrNull { it.id.glossaryId == selectedGlossaryId }
        val learnedCount = progress?.learnedCount ?: 0
        val progressRate = GlossaryProgressCalculator.progressRate(learnedCount, meta.termCount)

        return StudySelectedGlossary(
            glossaryId = meta.glossaryId,
            title = meta.title,
            learnedCount = learnedCount,
            totalTermCount = meta.termCount,
            level = GlossaryProgressCalculator.levelFromRate(progressRate)
        )
    }

    // 이번 주 자연주 (월요일 시작 ~ 일요일) 7일치
    private fun buildWeekStatistics(userId: UUID): StudyWeekStatistics {
        val today = LocalDate.now(clock.withZone(APP_ZONE_ID))
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)

        val rows = dailyProgressRepository
            .findByIdUserIdAndIdDateBetweenOrderByIdDateAsc(userId, monday, sunday)
            .associateBy { it.id.date }

        val days = (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            val row = rows[date]
            val active = row != null && (row.newLearnedCount + row.allLearnedCount) > 0
            StudyDailyStatistic(
                date = date,
                studied = active
            )
        }

        return StudyWeekStatistics(days = days)
    }

    companion object {
        // 자주 틀린 단어 최소 오답 횟수
        private const val FREQUENTLY_INCORRECT_MIN_COUNT: Int = 3
    }
}
