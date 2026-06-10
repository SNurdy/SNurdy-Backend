package com.snurdy.backend.domain.dashboard.study.service

import com.snurdy.backend.domain.dashboard.study.dto.StudyDailyStatistic
import com.snurdy.backend.domain.dashboard.study.dto.StudyDashboardResponse
import com.snurdy.backend.domain.dashboard.study.dto.StudyFrequentlyIncorrectTerm
import com.snurdy.backend.domain.dashboard.study.dto.StudySelectedGlossary
import com.snurdy.backend.domain.dashboard.study.dto.StudyWeekStatistics
import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryAvailableCountProjection
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.test.entity.TestType
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
    private val glossaryTermRepository: GlossaryTermRepository,
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
        val frequentlyIncorrectTerm = wrongAnswerTermRepository
            .findRandomFrequentlyIncorrect(userId, FREQUENTLY_INCORRECT_MIN_COUNT)
            ?.let {
                StudyFrequentlyIncorrectTerm(
                    termId = it.termId,
                    spelling = it.spelling,
                    meaning = it.meaning,
                    abbreviation = it.abbreviation,
                    totalIncorrectCount = it.totalIncorrectCount
                )
            }

        return StudyDashboardResponse(
            currentStreak = streak.currentStreak,
            selectedGlossary = selectedGlossary,
            defaultCardCount = preference.defaultCardCount,
            weekStatistics = weekStatistics,
            frequentlyIncorrectTerm = frequentlyIncorrectTerm,
            generatedAt = clock.instant()
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
            category = meta.category,
            title = meta.title,
            learnedCount = learnedCount,
            totalTermCount = meta.termCount,
            progressRate = progressRate,
            level = GlossaryProgressCalculator.levelFromRate(progressRate),
            lastLearnedAt = progress?.lastLearnedAt,
            availableCounts = buildAvailableCounts(glossaryTermRepository.countAvailableTerms(meta.glossaryId))
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
            val active = row != null && (row.learnedCount + row.completedCount) > 0
            StudyDailyStatistic(
                date = date,
                dayOfWeek = date.dayOfWeek,
                studied = active
            )
        }

        return StudyWeekStatistics(startDate = monday, endDate = sunday, days = days)
    }

    private fun buildAvailableCounts(counts: GlossaryAvailableCountProjection): Map<TestType, Int> {
        val total = counts.totalCount.toInt()
        val abbreviation = counts.abbreviationCount.toInt()
        return TestType.entries.associateWith { type ->
            when (type) {
                TestType.WRITE_ABBREVIATION, TestType.MC_PICK_ABBR -> abbreviation
                else -> total
            }
        }
    }

    companion object {
        // 자주 틀린 단어 최소 오답 횟수
        private const val FREQUENTLY_INCORRECT_MIN_COUNT: Int = 3
    }
}
