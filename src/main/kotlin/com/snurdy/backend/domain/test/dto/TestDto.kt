package com.snurdy.backend.domain.test.dto

import com.snurdy.backend.domain.test.entity.TestType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = "테스트 시작 요청")
data class StartTestRequest(
    @field:NotNull
    @field:Schema(description = "대상 단어장 id", example = "12")
    val glossaryId: Long,

    @field:NotNull
    @field:Schema(description = "테스트 유형", example = "MEANING_TO_TERM")
    val testType: TestType,

    @field:Min(1)
    @field:Max(50)
    @field:Schema(description = "출제 문제 수, 단어장 단어 수보다 크면 가능한 만큼만 출제", example = "30")
    val questionCount: Int
)

@Schema(description = "테스트 시작 응답")
data class StartTestResponse(
    val glossaryId: Long,
    val testType: TestType,
    @field:Schema(description = "출제된 문제 목록, 한 테스트 내 testType 은 모두 동일")
    val questions: List<TestQuestion>
)

@Schema(description = "테스트 문제 1개")
data class TestQuestion(
    val termId: Long,
    val prompt: TestPrompt,
    @field:Schema(description = "다지선다 보기 4개 (정답 포함, 순서 random), 작성형은 null")
    val choices: List<String>?,
    @field:Schema(description = "정답 문자열, 클라이언트가 사용자 입력과 비교해 정오 판정")
    val correctAnswer: String
)

@Schema(description = "문제 본문")
data class TestPrompt(
    val type: TestType,
    @field:Schema(description = "사용자에게 노출되는 문제 본문")
    val text: String
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
    @field:Schema(description = "이번 테스트로 레벨이 상승했을 때만 채워짐, 변화 없으면 null")
    val levelUp: LevelUpEvent?
)

@Schema(description = "단어장 레벨 상승 이벤트")
data class LevelUpEvent(
    val from: Int,
    val to: Int
)
