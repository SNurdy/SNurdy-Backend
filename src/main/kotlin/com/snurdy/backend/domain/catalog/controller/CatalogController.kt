package com.snurdy.backend.domain.catalog.controller

import com.snurdy.backend.domain.catalog.dto.GlossaryListResponse
import com.snurdy.backend.domain.catalog.dto.GlossaryTermsResponse
import com.snurdy.backend.domain.catalog.dto.TermDetailResponse
import com.snurdy.backend.domain.catalog.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.domain.catalog.service.CatalogQueryService
import com.snurdy.backend.global.constants.CatalogSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class CatalogController(
    private val catalogQueryService: CatalogQueryService
) : CatalogApi {

    override fun getGlossaries(): ApiResponse<GlossaryListResponse> =
        ApiResponse.success(CatalogSuccess.SEARCH, catalogQueryService.getGlossaries())

    override fun getGlossaryTerms(glossaryId: Long, page: Int, size: Int): ApiResponse<GlossaryTermsResponse> =
        ApiResponse.success(CatalogSuccess.SEARCH, catalogQueryService.getGlossaryTerms(glossaryId, page, size))

    override fun getTermDetail(termId: Long): ApiResponse<TermDetailResponse> =
        ApiResponse.success(CatalogSuccess.SEARCH, catalogQueryService.getTermDetail(termId))

    override fun searchTermsByMorpheme(keyword: String): ApiResponse<MorphemeSearchResponse> =
        ApiResponse.success(CatalogSuccess.SEARCH, catalogQueryService.searchTermsByMorpheme(keyword))
}
