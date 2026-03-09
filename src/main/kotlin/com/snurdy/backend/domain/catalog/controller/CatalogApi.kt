package com.snurdy.backend.domain.catalog.controller

import com.snurdy.backend.domain.catalog.dto.GlossaryListResponse
import com.snurdy.backend.domain.catalog.dto.GlossaryTermsResponse
import com.snurdy.backend.domain.catalog.dto.TermDetailResponse
import com.snurdy.backend.domain.catalog.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Validated
@Tag(name = "Catalog", description = "용어집/용어 조회 API")
@RequestMapping("/v1/catalog")
interface CatalogApi {

    @Operation(summary = "용어집 목록 조회")
    @GetMapping("/glossaries")
    fun getGlossaries(): ApiResponse<GlossaryListResponse>

    @Operation(summary = "용어집 용어 목록 조회")
    @GetMapping("/glossaries/{glossaryId}/terms")
    fun getGlossaryTerms(
        @PathVariable glossaryId: Long,
        @Parameter(description = "페이지 번호(0-base)", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<GlossaryTermsResponse>

    @Operation(summary = "용어 상세 조회 (ordered morphemes 포함)")
    @GetMapping("/terms/{termId}")
    fun getTermDetail(
        @PathVariable termId: Long
    ): ApiResponse<TermDetailResponse>

    @Operation(summary = "어근어미 키워드로 용어 검색")
    @GetMapping("/terms/search/morpheme")
    fun searchTermsByMorpheme(
        @Parameter(description = "어근어미 키워드 (최소 2자)", example = "hepat")
        @RequestParam @Size(
            min = 2, max = 50,
            message = "{com.snurdy.backend.global.constants.CatalogError.INVALID_SEARCH_KEYWORD}"
        ) keyword: String
    ): ApiResponse<MorphemeSearchResponse>
}
