package com.snurdy.backend.domain.card.dto

import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.test.entity.TestType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = "학습 카드 조회 요청")
data class CardsRequest(
    @field:NotNull
    @field:Positive
    @field:Schema(description = "대상 단어장 id", example = "12")
    val glossaryId: Long,

    @field:NotNull
    @field:Schema(description = "이어질 테스트 유형, 출제 단어 필터링 기준으로 사용", example = "MEANING_TO_TERM")
    val testType: TestType,

    @field:Min(1)
    @field:Max(50)
    @field:Schema(description = "조회할 카드 수, 단어장 단어 수보다 크면 가능한 만큼만 반환", example = "30")
    val count: Int
)

@Schema(description = "학습 카드 조회 응답")
data class CardsResponse(
    @field:Schema(description = "대상 단어장 id", example = "12")
    val glossaryId: Long,
    @field:Schema(description = "출제된 학습 카드 목록")
    val cards: List<Card>
)

@Schema(description = "학습 카드 1개")
data class Card(
    @field:Schema(description = "단어 id", example = "101")
    val termId: Long,
    @field:Schema(description = "영문 용어", example = "Electrocardiogram")
    val spelling: String,
    @field:Schema(description = "한글 의미", example = "심전도")
    val meaning: String,
    @field:Schema(description = "약어, 없으면 응답에서 키 제외", example = "ECG")
    val abbreviation: String?,
    @field:Schema(description = "상세 설명, 카드 ? 버튼 콘텐츠. 없으면 응답에서 키 제외", example = "심장 활동의 전기적 변화를 기록한 그래프")
    val explanation: String?,
    @field:Schema(description = "현재 학습 중인 단어장 안에서 사용자가 북마크 했는지 여부", example = "false")
    val bookmarked: Boolean,
    @field:Schema(description = "어근/어미 분석 + 형광펜 하이라이팅 범위. 색은 클라이언트가 order 순서대로 매핑")
    val morphemes: List<OrderedMorpheme>
)
