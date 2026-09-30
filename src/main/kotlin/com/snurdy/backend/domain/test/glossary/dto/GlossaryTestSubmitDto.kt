package com.snurdy.backend.domain.test.glossary.dto

import com.snurdy.backend.domain.test.common.TestType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = "기본 단어장 테스트 결과 제출 요청")
data class GlossaryTestSubmitRequest(
    @field:Valid
    @field:NotEmpty
    @field:Schema(description = "문제별 결과 목록")
    val results: List<GlossaryTestResultItem>,

    @field:Positive
    @field:Schema(description = "학습 화면에서 확인한 학습 세트 단어 수. study-cards 응답의 terms 개수를 전송. 생략 시 results 개수로 대체", example = "10")
    val studiedTermCount: Int? = null
)

@Schema(description = "테스트 문제 1개의 결과")
data class GlossaryTestResultItem(
    @field:Positive
    @field:Schema(description = "출제된 단어 id", example = "101")
    val termId: Long,
    @field:NotNull
    @field:Schema(description = "이 문제의 테스트 유형", example = "WRITE_TERM_FROM_MEANING")
    val testType: TestType,
    @field:Schema(description = "정답 여부, 클라이언트가 판정한 결과", example = "true")
    val correct: Boolean,
    @field:Schema(description = "사용자가 입력한 답안, 오답노트 표시용", example = "Electrocardiogram")
    val userAnswer: String
)

@Schema(description = "기본 단어장 테스트 제출 응답")
data class GlossaryTestSubmitResponse(
    @field:Schema(description = "정답 문제 수", example = "25")
    val correctCount: Int,
    @field:Schema(description = "오답 문제 수", example = "5")
    val incorrectCount: Int,
    @field:Schema(description = "이번 세션에서 학습한 단어 수", example = "10")
    val sessionStudiedCount: Int,
    @field:Schema(description = "오늘 기본 단어장 테스트로 처리한 누적 단어 수", example = "30")
    val todayStudiedCount: Int,
    @field:Schema(description = "전체 기간 기본 단어장 테스트로 처리한 누적 단어 수", example = "120")
    val totalStudiedCount: Int,
    @field:Schema(description = "일일 학습 목표 달성 여부", example = "true")
    val todayGoalCompleted: Boolean,
    @field:Schema(description = "테스트 전후 진척률/레벨 스냅샷")
    val glossary: GlossaryTestSnapshot
)

@Schema(description = "테스트 후 단어장 진척률/레벨 스냅샷")
data class GlossaryTestSnapshot(
    @field:Schema(description = "단어장 id", example = "12")
    val glossaryId: Long,
    @field:Schema(description = "테스트 전 진척률 0~100", example = "80.0")
    val progressRateBefore: Double,
    @field:Schema(description = "테스트 후 진척률 0~100", example = "85.0")
    val progressRateAfter: Double,
    @field:Schema(description = "테스트 전 레벨 1~5", example = "4")
    val levelBefore: Int,
    @field:Schema(description = "레벨 상승 시에만 채워짐. 변화 없으면 null")
    val levelUp: GlossaryLevelUpEvent?
)

@Schema(description = "단어장 레벨 상승 이벤트")
data class GlossaryLevelUpEvent(
    @field:Schema(description = "상승 전 레벨", example = "4")
    val from: Int,
    @field:Schema(description = "상승 후 레벨", example = "5")
    val to: Int
)