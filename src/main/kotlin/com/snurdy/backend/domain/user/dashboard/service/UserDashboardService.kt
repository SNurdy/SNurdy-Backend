package com.snurdy.backend.domain.user.dashboard.service

import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.user.dashboard.dto.DashboardDailyStatistic
import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryStatistic
import com.snurdy.backend.domain.user.dashboard.dto.DashboardPeriod
import com.snurdy.backend.domain.user.dashboard.dto.DashboardStreak
import com.snurdy.backend.domain.user.dashboard.dto.UserDashboardResponse
import com.snurdy.backend.domain.user.dashboard.dto.toDashboardStatistic
import com.snurdy.backend.domain.user.streak.service.UserStreakService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// 마이페이지의 학습 리포트 화면 BFF
// streak + glossary 별 진척도 + 날짜별 학습 지표를 한 응답으로 합친다
@Service
class UserDashboardService(
    private val userStreakService: UserStreakService,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val glossaryRepository: GlossaryRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getDashboard(userId: UUID, from: LocalDate?, to: LocalDate?): UserDashboardResponse {
        val today = LocalDate.now(clock.withZone(APP_ZONE_ID))
        val resolvedTo = to ?: today
        val resolvedFrom = from ?: resolvedTo.minusDays(DEFAULT_PERIOD_DAYS - 1)

        val streakResponse = userStreakService.getStreak(userId)
        val glossaryStatistics = buildGlossaryStatistics(userId)
        val dailyStatistics = buildDailyStatistics(userId, resolvedFrom, resolvedTo)

        return UserDashboardResponse(
            period = DashboardPeriod(from = resolvedFrom, to = resolvedTo),
            streak = DashboardStreak(
                currentStreak = streakResponse.currentStreak,
                longestStreak = streakResponse.longestStreak,
                totalLearningTerms = streakResponse.totalLearningTerms
            ),
            glossaryStatistics = glossaryStatistics,
            dailyStatistics = dailyStatistics,
            generatedAt = clock.instant()
        )
    }

    // 사용자가 학습 시작한 glossary 만 모음
    // 학습 안 한 glossary 는 대시보드에서 제외 (dictionary 메뉴에서 따로 봄)
    private fun buildGlossaryStatistics(userId: UUID): List<DashboardGlossaryStatistic> {
        val progresses = glossaryProgressRepository.findAllByIdUserId(userId)
        if (progresses.isEmpty()) return emptyList()

        val termCountByGlossaryId = glossaryRepository.findGlossaryTermCounts()
            .associateBy { it.glossaryId }

        return progresses.mapNotNull { progress ->
            val glossaryId = progress.id.glossaryId
            val meta = termCountByGlossaryId[glossaryId] ?: return@mapNotNull null
            val totalTermCount = meta.termCount
            val progressRate = GlossaryProgressCalculator.progressRate(progress.learnedCount, totalTermCount)
            DashboardGlossaryStatistic(
                glossaryId = glossaryId,
                category = meta.category,
                title = meta.title,
                learnedCount = progress.learnedCount,
                totalTermCount = totalTermCount,
                progressRate = progressRate,
                level = GlossaryProgressCalculator.levelFromRate(progressRate),
                lastLearnedAt = progress.lastLearnedAt
            )
        }.sortedByDescending { it.lastLearnedAt }
    }

    private fun buildDailyStatistics(userId: UUID, from: LocalDate, to: LocalDate): List<DashboardDailyStatistic> {
        return dailyProgressRepository
            .findByIdUserIdAndIdDateBetweenOrderByIdDateAsc(userId, from, to)
            .map { it.toDashboardStatistic() }
    }

    companion object {
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private const val DEFAULT_PERIOD_DAYS: Long = 30
    }
}
