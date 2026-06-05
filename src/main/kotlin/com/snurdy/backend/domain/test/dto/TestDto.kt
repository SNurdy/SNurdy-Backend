package com.snurdy.backend.domain.test.dto

import com.snurdy.backend.domain.test.entity.TestType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = "다지선다 보기 조회 요청")
data class DistractorsRequest(
    @field:NotNull
    @field:Positive
    @field:Schema(description = "대상 단어장 id", example = "12")
    val glossaryId: Long,

    @field:NotNull
    @field:Schema(description = "테스트 유형, MC_PICK_TERM / MC_PICK_MEANING / MC_PICK_ABBR 만 허용", example = "MC_PICK_TERM")
    val testType: TestType,

    @field:NotEmpty
    @field:Schema(description = "보기를 만들 단어 id 목록, cards 응답의 termId 를 그대로 전달")
    val termIds: List<Long>
)

@Schema(description = "다지선다 보기 조회 응답")
data class DistractorsResponse(
    @field:Schema(description = "termId 별 오답 보기 3개. 정답은 클라이언트가 cards 응답에서 derive 해 추가하고 shuffle")
    val distractorsByTermId: Map<Long, List<String>>
)

@Schema(description = "테스트 결과 일괄 제출 요청")
data class SubmitTestRequest(
    @field:NotNull
    @field:Positive
    @field:Schema(description = "대상 단어장 id", example = "12")
    val glossaryId: Long,

    @field:NotNull
    @field:Schema(description = "테스트 유형", example = "MEANING_TO_TERM")
    val testType: TestType,

    @field:Valid
    @field:NotEmpty
    @field:Schema(description = "문제별 결과 목록")
    val results: List<TestResultItem>
)

@Schema(description = "테스트 문제 1개의 결과")
data class TestResultItem(
    @field:Positive
    @field:Schema(description = "출제된 단어 id", example = "101")
    val termId: Long,
    @field:Schema(description = "정답 여부, 클라이언트가 판정한 결과", example = "true")
    val correct: Boolean,
    @field:Schema(description = "사용자가 입력한 답안, 오답노트 표시용", example = "Electrocardiogram")
    val userAnswer: String
)

@Schema(description = "테스트 제출 응답")
data class SubmitTestResponse(
    @field:Schema(description = "정답 문제 수")
    val correctCount: Int,
    @field:Schema(description = "오답 문제 수")
    val incorrectCount: Int,
    @field:Schema(description = "오늘 첫 테스트로 학습 진도가 반영되었는지 여부")
    val todayGoalCompleted: Boolean,
    val glossary: TestGlossarySnapshot
)

@Schema(description = "테스트 후 단어장 진척률/레벨 스냅샷")
data class TestGlossarySnapshot(
    val glossaryId: Long,
    @field:Schema(description = "테스트 전 진척률 0 부터 100")
    val progressRateBefore: Double,
    @field:Schema(description = "테스트 후 진척률 0 부터 100")
    val progressRateAfter: Double,
    @field:Schema(description = "테스트 전 레벨 1 부터 5")
    val levelBefore: Int,
    @field:Schema(description = "이번 테스트로 레벨이 상승했을 때만 채워짐, 변화 없으면 응답에서 키 제외")
    val levelUp: LevelUpEvent?
)

@Schema(description = "단어장 레벨 상승 이벤트")
data class LevelUpEvent(
    val from: Int,
    val to: Int
)
