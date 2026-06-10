package com.snurdy.backend.domain.dashboard.study.dto

import com.snurdy.backend.domain.test.entity.TestType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

@Schema(description = "학습하기 탭 대시보드 응답")
data class StudyDashboardResponse(
    @field:Schema(description = "현재 연속 학습 일수", example = "21")
    val currentStreak: Int,
    @field:Schema(description = "다마고치 영역에 표시할 선택된 과")
    val selectedGlossary: StudySelectedGlossary,
    @field:Schema(description = "한 세션 학습 카드 수 default", example = "10")
    val defaultCardCount: Int,
    @field:Schema(description = "이번 주 자연주 7일치 학습 여부")
    val weekStatistics: StudyWeekStatistics,
    @field:Schema(description = "[nullable] 자주 틀린 단어 카드. 3회 이상 오답 풀에서 랜덤 1개. 없으면 null")
    val frequentlyIncorrectTerm: StudyFrequentlyIncorrectTerm?,
    @field:Schema(description = "응답 생성 시각")
    val generatedAt: Instant
)

@Schema(description = "학습 Home 의 선택된 과")
data class StudySelectedGlossary(
    val glossaryId: Long,
    val category: String,
    val title: String,
    @field:Schema(description = "사용자가 학습 완료한 단어 수", example = "86")
    val learnedCount: Int,
    @field:Schema(description = "해당 과의 전체 단어 수", example = "100")
    val totalTermCount: Long,
    @field:Schema(description = "진척률 소수점 1자리", example = "86.0")
    val progressRate: Double,
    @field:Schema(description = "1 부터 5 까지 다마고치 레벨", example = "5")
    val level: Int,
    @field:Schema(description = "[nullable] 마지막 학습 시각. 학습 이력 없으면 null")
    val lastLearnedAt: Instant?,
    @field:Schema(
        description = "테스트하기 셀렉터 최대치. testType 별 가용 단어 수",
        example = """{"TERM_TO_MEANING":100,"MEANING_TO_TERM":100,"LISTEN_AND_WRITE":100,"WRITE_ABBREVIATION":42,"MC_PICK_TERM":100,"MC_PICK_MEANING":100,"MC_PICK_ABBR":42}"""
    )
    val availableCounts: Map<TestType, Int>
)

@Schema(description = "이번 주 자연주 7일치 학습 여부")
data class StudyWeekStatistics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<StudyDailyStatistic>
)

@Schema(description = "일별 학습 여부")
data class StudyDailyStatistic(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    @field:Schema(description = "그 날 테스트 submit 1회 이상이면 true (캘린더 체크 표시 기준)", example = "true")
    val studied: Boolean
)

@Schema(description = "자주 틀린 단어 카드")
data class StudyFrequentlyIncorrectTerm(
    val termId: Long,
    val spelling: String,
    val meaning: String,
    @field:Schema(description = "[nullable] 약어")
    val abbreviation: String?,
    @field:Schema(description = "누적 오답 횟수", example = "5")
    val totalIncorrectCount: Int
)
