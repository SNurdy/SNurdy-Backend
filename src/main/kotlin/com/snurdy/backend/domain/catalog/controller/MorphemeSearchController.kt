package com.snurdy.backend.domain.catalog.controller

import com.snurdy.backend.domain.catalog.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.domain.catalog.service.CatalogQueryService
import com.snurdy.backend.global.constants.CatalogSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class MorphemeSearchController(
    private val catalogQueryService: CatalogQueryService
) : MorphemeSearchApi {

    override fun searchTermsByMorpheme(keyword: String): ApiResponse<MorphemeSearchResponse> =
        ApiResponse.success(CatalogSuccess.SEARCH, catalogQueryService.searchTermsByMorpheme(keyword))
}
