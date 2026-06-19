package com.snurdy.backend.domain.admin.dictionary.dto

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.morpheme.entity.MorphemeEntity
import com.snurdy.backend.domain.dictionary.morpheme.entity.MorphemeVariantEntity
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.OrderedMorphemeProjection
import org.springframework.data.domain.Page

data class GlossaryPageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<GlossaryItem>
)

data class GlossaryItem(
    val id: Long,
    val category: String,
    val title: String
)

data class TermPageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<TermItem>
)

data class TermItem(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val explanation: String?
)

data class TermDetailResponse(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val explanation: String?,
    val glossaryIds: List<Long>,
    val morphemes: List<TermMorphemeMappingResponse>
)

data class TermMorphemeMappingResponse(
    val morphemeId: Long,
    val spelling: String,
    val meaning: String,
    val position: Int
)

data class MorphemePageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<MorphemeItem>
)

data class MorphemeItem(
    val id: Long,
    val spelling: String,
    val meaning: String
)

data class MorphemeVariantPageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<MorphemeVariantItem>
)

data class MorphemeVariantItem(
    val id: Long,
    val morphemeId: Long,
    val variant: String,
    val base: String
)

fun GlossaryEntity.toResponse() = GlossaryItem(
    id = id,
    category = category,
    title = title
)

fun Page<GlossaryEntity>.toResponse() = GlossaryPageResponse(
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    items = content.map { it.toResponse() }
)

// TermEntity 는 dictionary 도메인의 toSummary/toDetail 과 다른 응답으로도 변환되어 1:N 케이스 - 의미 키워드로 구분
fun TermEntity.toItem() = TermItem(
    id = id,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation
)

fun Page<TermEntity>.toResponse() = TermPageResponse(
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    items = content.map { it.toItem() }
)

fun TermEntity.toDetail(
    glossaryIds: List<Long>,
    morphemes: List<TermMorphemeMappingResponse>
) = TermDetailResponse(
    id = id,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation,
    glossaryIds = glossaryIds,
    morphemes = morphemes
)

fun OrderedMorphemeProjection.toResponse() = TermMorphemeMappingResponse(
    morphemeId = morphemeId,
    spelling = spelling,
    meaning = meaning,
    position = position
)

fun MorphemeEntity.toResponse() = MorphemeItem(
    id = id,
    spelling = spelling,
    meaning = meaning
)

fun Page<MorphemeEntity>.toResponse() = MorphemePageResponse(
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    items = content.map { it.toResponse() }
)

fun MorphemeVariantEntity.toResponse() = MorphemeVariantItem(
    id = id,
    morphemeId = morpheme.id,
    variant = variant,
    base = base
)

fun Page<MorphemeVariantEntity>.toResponse() = MorphemeVariantPageResponse(
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    items = content.map { it.toResponse() }
)
