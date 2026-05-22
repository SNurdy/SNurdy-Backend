package com.snurdy.backend.domain.dictionary.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "용어 상세 응답")
data class TermDetailResponse(
    @field:Schema(description = "용어 정보")
    val term: TermDetail,
    @field:Schema(description = "용어가 포함된 용어집(해시태그 표시용)")
    val glossaries: List<TermGlossaryTag>,
    @field:Schema(description = "순서가 보장된 어근어미 목록")
    val morphemes: List<OrderedMorpheme>
)

@Schema(description = "용어 상세 내 용어집 태그")
data class TermGlossaryTag(
    @field:Schema(description = "용어집 ID", example = "6")
    val id: Long,
    @field:Schema(description = "용어집 카테고리", example = "내과")
    val category: String,
    @field:Schema(description = "용어집 제목", example = "심장내과")
    val title: String
)

@Schema(description = "용어 상세")
data class TermDetail(
    @field:Schema(description = "용어 ID", example = "618")
    val id: Long,
    @field:Schema(description = "철자", example = "Hepatitis")
    val spelling: String,
    @field:Schema(description = "뜻", example = "간염")
    val meaning: String,
    @field:Schema(description = "약어", example = "")
    val abbreviation: String?,
    @field:Schema(description = "설명")
    val explanation: String?
)


@Schema(description = "순서 포함 어근어미")
data class OrderedMorpheme(
    @field:Schema(description = "어근어미 ID", example = "361")
    val id: Long,
    @field:Schema(description = "어근어미 철자", example = "hepat(o)-")
    val spelling: String,
    @field:Schema(description = "어근어미 의미", example = "간")
    val meaning: String,
    @field:Schema(description = "용어 내 순서", example = "0")
    val order: Int,
    @field:Schema(description = "하이라이팅 범위 (null이면 매칭 실패)")
    val highlight: MorphemeHighlight? = null
)

@Schema(description = "어근어미 하이라이팅 범위")
data class MorphemeHighlight(
    @field:Schema(description = "하이라이팅 시작 인덱스 (0-based)", example = "0")
    val start: Int,
    @field:Schema(description = "하이라이팅 길이", example = "6")
    val length: Int
)
