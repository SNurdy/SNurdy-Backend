package com.snurdy.backend.domain.user.dashboard.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

@Schema(description = "v2 학습 리포트 대시보드 응답")
data class UserDashboardResponse(
    val period: DashboardPeriod,
    val streak: DashboardStreak,
    val glossaryStatistics: List<DashboardGlossaryStatistic>,
    val dailyStatistics: List<DashboardDailyStatistic>,
    val generatedAt: Instant
)

@Schema(description = "대시보드 조회 기간")
data class DashboardPeriod(
    val from: LocalDate,
    val to: LocalDate
)

@Schema(description = "사용자 학습 streak 요약")
data class DashboardStreak(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalLearningTerms: Int
)

@Schema(description = "용어집(과목)별 학습 지표")
data class DashboardGlossaryStatistic(
    val glossaryId: Long,
    val category: String,
    val title: String,
    val learnedCount: Int,
    val totalTermCount: Long,
    val progressRate: Double,
    val lastLearnedAt: Instant?
)

@Schema(description = "날짜별 학습/복습 지표")
data class DashboardDailyStatistic(
    val date: LocalDate,
    val learnedCount: Int,
    val reviewedCount: Int
)
