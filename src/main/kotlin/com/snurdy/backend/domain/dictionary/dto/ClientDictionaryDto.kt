package com.snurdy.backend.domain.dictionary.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "앱용 용어 검색 응답")
data class TermSearchResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<ClientTermSummary>
)

@Schema(description = "앱용 용어 요약")
data class ClientTermSummary(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val bookmarked: Boolean
)

@Schema(description = "앱용 용어 상세")
data class ClientTermDetailResponse(
    val term: TermDetail,
    val glossaries: List<TermGlossaryTag>,
    val morphemes: List<OrderedMorpheme>,
    val bookmarked: Boolean,
    val bookmarkedGlossaryIds: List<Long>,
    @field:Schema(description = "본인이 이 단어에 작성한 메모. 없으면 null", example = "혈전 관련 검사 항목")
    val memo: String?
)

@Schema(description = "앱용 용어장 목록 응답")
data class ClientGlossaryListResponse(
    val glossaries: List<ClientGlossarySummary>
)

@Schema(description = "앱용 용어장 요약")
data class ClientGlossarySummary(
    val id: Long,
    val category: String,
    val title: String,
    val termCount: Long,
    val learnedCount: Int,
    val progressRate: Double,
    val level: Int
)

@Schema(description = "앱용 용어장 용어 목록 응답")
data class ClientGlossaryTermsResponse(
    val glossaryId: Long,
    val category: String,
    val title: String,
    val filter: String,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<ClientGlossaryTermItem>
)

@Schema(description = "앱용 용어장 용어 항목")
data class ClientGlossaryTermItem(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val status: String,
    val bookmarked: Boolean
)
