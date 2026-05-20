package com.snurdy.backend.domain.user.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.service.LearningQueryService
import com.snurdy.backend.domain.user.dto.DashboardDailyStat
import com.snurdy.backend.domain.user.dto.DashboardGlossaryStat
import com.snurdy.backend.domain.user.dto.DashboardPeriod
import com.snurdy.backend.domain.user.dto.DashboardStreak
import com.snurdy.backend.domain.user.dto.UserDashboardResponse
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
    private val learningQueryService: LearningQueryService,
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

        val streakResponse = learningQueryService.getStreak(userId)
        val glossaryStats = buildGlossaryStats(userId)
        val dailyStats = buildDailyStats(userId, resolvedFrom, resolvedTo)

        return UserDashboardResponse(
            period = DashboardPeriod(from = resolvedFrom, to = resolvedTo),
            streak = DashboardStreak(
                currentStreak = streakResponse.currentStreak,
                longestStreak = streakResponse.longestStreak,
                totalLearningTerms = streakResponse.totalLearningTerms
            ),
            glossaryStats = glossaryStats,
            dailyStats = dailyStats,
            generatedAt = clock.instant()
        )
    }

    // 사용자가 학습 시작한 glossary 만 모음
    // 학습 안 한 glossary 는 대시보드에서 제외 (dictionary 메뉴에서 따로 봄)
    private fun buildGlossaryStats(userId: UUID): List<DashboardGlossaryStat> {
        val progresses = glossaryProgressRepository.findAllByIdUserId(userId)
        if (progresses.isEmpty()) return emptyList()

        val termCountByGlossaryId = glossaryRepository.findGlossaryTermCounts()
            .associateBy { it.glossaryId }

        return progresses.mapNotNull { progress ->
            val glossaryId = progress.id.glossaryId
            val meta = termCountByGlossaryId[glossaryId] ?: return@mapNotNull null
            val totalTermCount = meta.termCount
            val progressRate = if (totalTermCount == 0L) {
                0.0
            } else {
                ((progress.learnedCount.toDouble() / totalTermCount.toDouble()) * 1000.0)
                    .toInt() / 10.0
            }
            DashboardGlossaryStat(
                glossaryId = glossaryId,
                category = meta.category,
                title = meta.title,
                learnedCount = progress.learnedCount,
                totalTermCount = totalTermCount,
                progressRate = progressRate,
                lastLearnedAt = progress.lastLearnedAt
            )
        }.sortedByDescending { it.lastLearnedAt }
    }

    private fun buildDailyStats(userId: UUID, from: LocalDate, to: LocalDate): List<DashboardDailyStat> {
        return dailyProgressRepository
            .findByIdUserIdAndIdDateBetweenOrderByIdDateAsc(userId, from, to)
            .map {
                DashboardDailyStat(
                    date = it.id.date,
                    learnedCount = it.learnedCount,
                    reviewedCount = it.reviewedCount
                )
            }
    }

    companion object {
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private const val DEFAULT_PERIOD_DAYS: Long = 30
    }
}
