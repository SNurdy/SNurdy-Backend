package com.snurdy.backend.domain.catalog.controller

import com.snurdy.backend.domain.catalog.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.catalog.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.catalog.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.catalog.dto.TermSearchResponse
import com.snurdy.backend.domain.catalog.service.ClientCatalogService
import com.snurdy.backend.global.constants.CatalogSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.extension.currentUserId
import org.springframework.web.bind.annotation.RestController

@RestController
class ClientCatalogController(
    private val clientCatalogService: ClientCatalogService
) : ClientCatalogApi {
    override fun searchTerms(query: String?, page: Int, size: Int): ApiResponse<TermSearchResponse> =
        ApiResponse.success(
            CatalogSuccess.SEARCH,
            clientCatalogService.searchTerms(currentUserId(), query, page, size)
        )

    override fun getTermDetail(termId: Long): ApiResponse<ClientTermDetailResponse> =
        ApiResponse.success(
            CatalogSuccess.SEARCH,
            clientCatalogService.getTermDetail(currentUserId(), termId)
        )

    override fun getGlossaries(category: String?): ApiResponse<ClientGlossaryListResponse> =
        ApiResponse.success(
            CatalogSuccess.SEARCH,
            clientCatalogService.getGlossaries(currentUserId(), category)
        )

    override fun getGlossaryTerms(
        glossaryId: Long,
        filter: String,
        page: Int,
        size: Int
    ): ApiResponse<ClientGlossaryTermsResponse> =
        ApiResponse.success(
            CatalogSuccess.SEARCH,
            clientCatalogService.getGlossaryTerms(
                userId = currentUserId(),
                glossaryId = glossaryId,
                filter = filter,
                page = page,
                size = size
            )
        )
}
