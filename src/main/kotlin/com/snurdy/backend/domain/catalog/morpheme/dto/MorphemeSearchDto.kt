package com.snurdy.backend.domain.catalog.morpheme.dto

import com.snurdy.backend.domain.catalog.dto.MorphemeHighlight
import com.snurdy.backend.domain.catalog.dto.TermGlossaryTag
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "형태소 검색 응답")
data class MorphemeSearchResponse(
    @field:Schema(description = "검색 키워드", example = "hepat")
    val keyword: String,
    @field:Schema(description = "검색 결과 수", example = "3")
    val totalCount: Int,
    @field:Schema(description = "검색 결과 단어 목록")
    val terms: List<MorphemeSearchTermItem>
)

@Schema(description = "형태소 검색 결과 단어 항목")
data class MorphemeSearchTermItem(
    @field:Schema(description = "단어 ID", example = "618")
    val id: Long,
    @field:Schema(description = "철자", example = "Hepatitis")
    val spelling: String,
    @field:Schema(description = "뜻", example = "간염")
    val meaning: String,
    @field:Schema(description = "약어")
    val abbreviation: String?,
    @field:Schema(description = "단어가 포함된 단어장 목록")
    val glossaries: List<TermGlossaryTag>,
    @field:Schema(description = "순서가 보장된 형태소 목록")
    val morphemes: List<SearchOrderedMorpheme>
)

@Schema(description = "검색 결과용 형태소 (매칭 여부 포함)")
data class SearchOrderedMorpheme(
    @field:Schema(description = "형태소 ID", example = "361")
    val id: Long,
    @field:Schema(description = "형태소 철자", example = "hepat(o)-")
    val spelling: String,
    @field:Schema(description = "형태소 의미", example = "간")
    val meaning: String,
    @field:Schema(description = "단어 내 순서", example = "0")
    val order: Int,
    @field:Schema(description = "하이라이팅 범위")
    val highlight: MorphemeHighlight? = null,
    @field:Schema(description = "이 검색에서 매칭된 형태소 여부", example = "true")
    val matched: Boolean = false
)