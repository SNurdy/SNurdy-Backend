package com.snurdy.backend.domain.dictionary.controller

import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.dictionary.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.domain.dictionary.service.GlossaryListingService
import com.snurdy.backend.domain.dictionary.service.TermDetailService
import com.snurdy.backend.domain.dictionary.service.TermSearchService
import com.snurdy.backend.global.constants.DictionarySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ClientDictionaryController(
    private val termSearchService: TermSearchService,
    private val termDetailService: TermDetailService,
    private val glossaryListingService: GlossaryListingService
) : ClientDictionaryApi {
    override fun searchTerms(userId: UUID, query: String?, page: Int, size: Int): ApiResponse<TermSearchResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH_TERMS,
            termSearchService.searchTerms(userId, query, page, size)
        )

    override fun getTermDetail(userId: UUID, termId: Long): ApiResponse<ClientTermDetailResponse> =
        ApiResponse.success(
            DictionarySuccess.GET_TERM_DETAIL,
            termDetailService.getTermDetail(userId, termId)
        )

    override fun getGlossaries(userId: UUID, category: String?): ApiResponse<ClientGlossaryListResponse> =
        ApiResponse.success(
            DictionarySuccess.GET_GLOSSARIES,
            glossaryListingService.getGlossaries(userId, category)
        )

    override fun getGlossaryTerms(
        userId: UUID,
        glossaryId: Long,
        filter: String,
        page: Int,
        size: Int
    ): ApiResponse<ClientGlossaryTermsResponse> =
        ApiResponse.success(
            DictionarySuccess.GET_GLOSSARY_TERMS,
            glossaryListingService.getGlossaryTerms(
                userId = userId,
                glossaryId = glossaryId,
                filter = filter,
                page = page,
                size = size
            )
        )
}
