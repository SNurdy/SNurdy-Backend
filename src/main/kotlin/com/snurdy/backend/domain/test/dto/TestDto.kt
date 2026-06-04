package com.snurdy.backend.domain.test.dto

import com.snurdy.backend.domain.test.entity.TestType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

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
