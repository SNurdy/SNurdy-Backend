package com.snurdy.backend.domain.study.glossary.dto

import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = "과별 단어장 학습 카드 요청")
data class GlossaryStudyCardsRequest(
    @field:NotNull
    @field:Min(1)
    @field:Max(50)
    @field:Schema(description = "학습할 단어 수 (10/20/30/40/50)", example = "10")
    val count: Int
)

@Schema(description = "과별 단어장 학습 카드 응답")
data class GlossaryStudyCardsResponse(
    @field:Schema(description = "클라이언트가 요청한 단어 수", example = "10")
    val requestedCount: Int,

    @field:Schema(description = "학습 화면에 노출할 학습 세트 단어. 단어장 잔여 단어가 requestedCount 보다 적으면 그만큼만 내려감")
    val terms: List<GlossaryStudyTerm>
)

@Schema(description = "과별 단어장 학습 세트 단어 1개")
data class GlossaryStudyTerm(
    @field:Schema(description = "단어 id", example = "293")
    val termId: Long,
    @field:Schema(description = "영문 용어", example = "arterial line")
    val spelling: String,
    @field:Schema(description = "한글 의미", example = "동맥선, 동맥 카테터")
    val meaning: String,
    @field:Schema(description = "약어. 없으면 null", example = "A-Line")
    val abbreviation: String?,
    @field:Schema(description = "상세 설명. 없으면 null", example = "지속적인 혈압 감시 및 동맥혈 채취를 위해 동맥 내에 삽입하는 카테터입니다.")
    val explanation: String?,
    @field:Schema(description = "현재 학습 중인 단어장 안에서 사용자가 북마크 했는지 여부", example = "false")
    val bookmarked: Boolean,
    @field:Schema(description = "어근/어미 분석 + 형광펜 하이라이팅 범위. 색은 클라이언트가 order 순서대로 매핑")
    val morphemes: List<OrderedMorpheme>
)

fun SimpleTermProjection.toStudyTerm(
    bookmarked: Boolean,
    morphemes: List<OrderedMorpheme>
): GlossaryStudyTerm = GlossaryStudyTerm(
    termId = termId,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation,
    bookmarked = bookmarked,
    morphemes = morphemes
)