package com.snurdy.backend.domain.dashboard.study.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
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
    @field:Schema(description = "자주 틀린 단어 카드. 최근 학습한 단어장의 3회 이상 오답 풀에서 랜덤 1개. 없으면 null")
    val frequentlyIncorrectTerm: StudyFrequentlyIncorrectTerm?
)

@Schema(description = "학습 Home 의 선택된 과")
data class StudySelectedGlossary(
    val glossaryId: Long,
    val title: String,
    @field:Schema(description = "사용자가 학습 완료한 단어 수", example = "86")
    val learnedCount: Int,
    @field:Schema(description = "해당 과의 전체 단어 수", example = "100")
    val totalTermCount: Long,
    @field:Schema(description = "1 부터 5 까지 다마고치 레벨", example = "5")
    val level: Int
)

@Schema(description = "이번 주 자연주 7일치 학습 여부")
data class StudyWeekStatistics(
    @field:ArraySchema(
        schema = Schema(implementation = StudyDailyStatistic::class),
        arraySchema = Schema(
            description = "월요일부터 일요일까지 7일치. 항상 7개 보장",
            example = """[
                {"date":"2026-06-08","studied":true},
                {"date":"2026-06-09","studied":false},
                {"date":"2026-06-10","studied":false},
                {"date":"2026-06-11","studied":false},
                {"date":"2026-06-12","studied":false},
                {"date":"2026-06-13","studied":false},
                {"date":"2026-06-14","studied":false}
            ]"""
        )
    )
    val days: List<StudyDailyStatistic>
)

@Schema(description = "일별 학습 여부")
data class StudyDailyStatistic(
    val date: LocalDate,
    @field:Schema(description = "그 날 테스트 submit 1회 이상이면 true", example = "true")
    val studied: Boolean
)

@Schema(description = "자주 틀린 단어 카드")
data class StudyFrequentlyIncorrectTerm(
    val termId: Long,
    val spelling: String,
    val meaning: String,
    @field:Schema(description = "약어. 없으면 null")
    val abbreviation: String?
)
