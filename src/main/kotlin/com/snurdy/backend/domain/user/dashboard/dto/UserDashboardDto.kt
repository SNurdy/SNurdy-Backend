package com.snurdy.backend.domain.user.dashboard.dto

import com.snurdy.backend.domain.test.entity.TestType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

@Schema(description = "마이페이지 메인 대시보드 응답")
data class UserDashboardResponse(
    @field:Schema(description = "사용자 streak 요약")
    val streak: DashboardStreak,
    @field:Schema(description = "다마고치 영역에 표시할 선택된 과. 학습 시작한 과가 없으면 null")
    val selectedGlossary: DashboardSelectedGlossary?,
    @field:Schema(description = "7일치 학습 막대 그래프 데이터")
    val weekStatistics: DashboardWeekStatistics,
    @field:Schema(description = "응답 생성 시각")
    val generatedAt: Instant
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
    val category: String,
    val title: String,
    @field:Schema(description = "사용자가 학습 완료한 단어 수", example = "85")
    val learnedCount: Int,
    @field:Schema(description = "해당 과의 전체 단어 수", example = "100")
    val totalTermCount: Long,
    @field:Schema(description = "진척률 소수점 1자리", example = "85.0")
    val progressRate: Double,
    @field:Schema(description = "1 부터 5 까지 다마고치 레벨", example = "5")
    val level: Int,
    val lastLearnedAt: Instant?,
    @field:Schema(
        description = "테스트하기 단어 수 selector 의 최대치로 사용. testType 별 가용 단어 수. " +
            "WRITE_ABBREVIATION / MC_PICK_ABBR 은 abbreviation 보유 단어 수, 나머지는 전체 단어 수",
        example = """{"TERM_TO_MEANING":100,"MEANING_TO_TERM":100,"LISTEN_AND_WRITE":100,"WRITE_ABBREVIATION":42,"MC_PICK_TERM":100,"MC_PICK_MEANING":100,"MC_PICK_ABBR":42}"""
    )
    val availableCounts: Map<TestType, Int>
)

@Schema(description = "7일치 막대 그래프 데이터")
data class DashboardWeekStatistics(
    @field:Schema(description = "조회 시작일", example = "2026-05-29")
    val startDate: LocalDate,
    @field:Schema(description = "조회 종료일 (startDate + 6)", example = "2026-06-04")
    val endDate: LocalDate,
    @field:Schema(description = "7일치 일별 통계. 학습 안 한 날도 0 으로 채워 항상 7개 보장")
    val days: List<DashboardDailyStatistic>
)

@Schema(description = "일별 학습 복습 지표")
data class DashboardDailyStatistic(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    @field:Schema(description = "그날 학습한 단어 수", example = "10")
    val learnedCount: Int,
    @field:Schema(description = "그날 복습 처리한 단어 수", example = "8")
    val completedCount: Int
)

@Schema(description = "과 변경 바텀시트 응답")
data class DashboardGlossaryListResponse(
    @field:Schema(description = "전체 27개 과 카탈로그. 학습 시작 안 한 과는 learnedCount=0")
    val glossaries: List<DashboardGlossaryListItem>
)

@Schema(description = "과 변경 바텀시트 항목")
data class DashboardGlossaryListItem(
    val glossaryId: Long,
    val category: String,
    val title: String,
    val learnedCount: Int,
    val totalTermCount: Long,
    val progressRate: Double,
    val level: Int,
    val lastLearnedAt: Instant?
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
    val category: String,
    val title: String,
    @field:Schema(description = "누적 학습 완료 단어 수 (period 영향 없음)")
    val learnedCount: Int,
    val totalTermCount: Long,
    @field:Schema(description = "누적 진척률 (period 영향 없음)")
    val progressRate: Double,
    @field:Schema(description = "누적 다마고치 레벨 (period 영향 없음)")
    val level: Int,
    @field:Schema(description = "period 기간 동안 학습한 단어 수", example = "49")
    val periodLearnedTermCount: Long,
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
