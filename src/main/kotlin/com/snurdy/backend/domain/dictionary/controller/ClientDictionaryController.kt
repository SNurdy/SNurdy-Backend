package com.snurdy.backend.domain.dictionary.controller

import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.dictionary.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.domain.dictionary.service.ClientDictionaryService
import com.snurdy.backend.global.constants.DictionarySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.extension.currentUserId
import org.springframework.web.bind.annotation.RestController

@RestController
class ClientDictionaryController(
    private val clientDictionaryService: ClientDictionaryService
) : ClientDictionaryApi {
    override fun searchTerms(query: String?, page: Int, size: Int): ApiResponse<TermSearchResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH,
            clientDictionaryService.searchTerms(currentUserId(), query, page, size)
        )

    override fun getTermDetail(termId: Long): ApiResponse<ClientTermDetailResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH,
            clientDictionaryService.getTermDetail(currentUserId(), termId)
        )

    override fun getGlossaries(category: String?): ApiResponse<ClientGlossaryListResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH,
            clientDictionaryService.getGlossaries(currentUserId(), category)
        )

    override fun getGlossaryTerms(
        glossaryId: Long,
        filter: String,
        page: Int,
        size: Int
    ): ApiResponse<ClientGlossaryTermsResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH,
            clientDictionaryService.getGlossaryTerms(
                userId = currentUserId(),
                glossaryId = glossaryId,
                filter = filter,
                page = page,
                size = size
            )
        )
}
