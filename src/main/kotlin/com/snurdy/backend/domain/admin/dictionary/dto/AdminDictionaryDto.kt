package com.snurdy.backend.domain.admin.dictionary.dto

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
