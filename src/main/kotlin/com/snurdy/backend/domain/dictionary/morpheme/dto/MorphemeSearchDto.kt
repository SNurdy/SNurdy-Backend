package com.snurdy.backend.domain.dictionary.morpheme.dto

import com.snurdy.backend.domain.dictionary.dto.TermGlossaryTag
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "어근어미 검색 응답")
data class MorphemeSearchResponse(
    @field:Schema(description = "검색 키워드", example = "hepat")
    val keyword: String,
    @field:Schema(description = "검색 결과 수", example = "3")
    val totalCount: Int,
    @field:Schema(description = "검색 결과 용어 목록")
    val terms: List<MorphemeSearchTermItem>
)

@Schema(description = "어근어미 검색 결과 용어 항목")
data class MorphemeSearchTermItem(
    @field:Schema(description = "용어 ID", example = "618")
    val id: Long,
    @field:Schema(description = "철자", example = "Hepatitis")
    val spelling: String,
    @field:Schema(description = "뜻", example = "간염")
    val meaning: String,
    @field:Schema(description = "약어")
    val abbreviation: String?,
    @field:Schema(description = "용어가 포함된 용어집 목록")
    val glossaries: List<TermGlossaryTag>
)

// TermEntity 의 또 다른 응답 변환 (Summary/Detail 외 추가) - 1:N 케이스
fun TermEntity.toMorphemeSearchItem(glossaries: List<TermGlossaryTag>) = MorphemeSearchTermItem(
    id = id,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    glossaries = glossaries
)
