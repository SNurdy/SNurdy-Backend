package com.snurdy.backend.domain.dashboard.me.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalDate

@Schema(description = "마이페이지 탭 메인 대시보드 응답")
data class MyDashboardResponse(
    @field:Schema(description = "사용자 streak 요약")
    val streak: DashboardStreak,
    @field:Schema(description = "다마고치 영역에 표시할 선택된 과. 사용자가 명시 선택한 값 (신규 유저는 가입 시점에 심장내과 자동 세팅)")
    val selectedGlossary: DashboardSelectedGlossary,
    @field:Schema(description = "7일치 학습 막대 그래프 데이터")
    val weekStatistics: DashboardWeekStatistics
)

@Schema(description = "사용자 학습 streak 요약")
data class DashboardStreak(
    @field:Schema(description = "최장 연속 학습 일수", example = "21")
    val longestStreak: Int,
    @field:Schema(description = "지금까지 학습한 총 단어 수", example = "340")
    val totalLearningTerms: Int
)

@Schema(description = "메인 다마고치 영역에 표시할 선택된 과")
data class DashboardSelectedGlossary(
    val glossaryId: Long,
    val title: String,
    @field:Schema(description = "사용자가 학습 완료한 단어 수", example = "85")
    val learnedCount: Int,
    @field:Schema(description = "해당 과의 전체 단어 수", example = "100")
    val totalTermCount: Long,
    @field:Schema(description = "1 부터 5 까지 다마고치 레벨", example = "5")
    val level: Int
)

@Schema(description = "7일치 막대 그래프 데이터")
data class DashboardWeekStatistics(
    @field:Schema(description = "조회 시작일", example = "2026-05-29")
    val startDate: LocalDate,
    @field:Schema(description = "조회 종료일 (startDate + 6)", example = "2026-06-04")
    val endDate: LocalDate,
    @field:ArraySchema(
        schema = Schema(implementation = DashboardDailyStatistic::class),
        arraySchema = Schema(
            description = "7일치 일별 통계. 학습 안 한 날도 0 으로 채워 항상 7개 보장",
            example = """[
                {"date":"2026-05-29","dayOfWeek":"FRIDAY","newLearnedCount":10},
                {"date":"2026-05-30","dayOfWeek":"SATURDAY","newLearnedCount":5},
                {"date":"2026-05-31","dayOfWeek":"SUNDAY","newLearnedCount":0},
                {"date":"2026-06-01","dayOfWeek":"MONDAY","newLearnedCount":15},
                {"date":"2026-06-02","dayOfWeek":"TUESDAY","newLearnedCount":7},
                {"date":"2026-06-03","dayOfWeek":"WEDNESDAY","newLearnedCount":0},
                {"date":"2026-06-04","dayOfWeek":"THURSDAY","newLearnedCount":3}
            ]"""
        )
    )
    val days: List<DashboardDailyStatistic>
)

@Schema(description = "일별 신규 학습 지표")
data class DashboardDailyStatistic(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    @field:Schema(description = "그 날 신규로 학습한 단어 수", example = "10")
    val newLearnedCount: Int
)

@Schema(description = "과 변경 바텀시트 응답")
data class DashboardGlossaryListResponse(
    @field:Schema(description = "전체 27개 과 카탈로그. 학습 시작 안 한 과는 learnedCount=0")
    val glossaries: List<DashboardGlossaryListItem>
)

@Schema(description = "과 변경 바텀시트 항목")
data class DashboardGlossaryListItem(
    val glossaryId: Long,
    @field:Schema(description = "카테고리 탭 그룹화 키", example = "외과")
    val category: String,
    val title: String,
    @field:Schema(description = "사용자가 학습 완료한 단어 수", example = "85")
    val learnedCount: Int,
    @field:Schema(description = "해당 과의 전체 단어 수", example = "100")
    val totalTermCount: Long
)

@Schema(description = "과 지표 리스트 응답 (학습 시작한 과만, lastLearnedAt 최근순)")
data class DashboardGlossaryStatisticsResponse(
    @field:Schema(description = "조회 기간", example = "WEEK")
    val period: DashboardGlossaryPeriod,
    val glossaries: List<DashboardGlossaryStatisticItem>
)

@Schema(description = "과 지표 리스트 항목")
data class DashboardGlossaryStatisticItem(
    val glossaryId: Long,
    val title: String,
    @field:Schema(description = "누적 학습 완료 단어 수. period 영향 없음", example = "85")
    val learnedCount: Int,
    @field:Schema(description = "해당 과의 전체 단어 수", example = "100")
    val totalTermCount: Long,
    @field:Schema(description = "누적 다마고치 레벨. period 영향 없음", example = "5")
    val level: Int,
    @field:Schema(description = "period 기간 동안 오답이 발생한 단어 종 수", example = "232")
    val periodWrongAnswerCount: Long
)

@Schema(description = "과 지표 상세 조회 기간")
enum class DashboardGlossaryPeriod {
    @Schema(description = "이번 주 자연주 (월요일 00:00 ~ 오늘 23:59:59)")
    WEEK,

    @Schema(description = "전체 누적")
    ALL
}
