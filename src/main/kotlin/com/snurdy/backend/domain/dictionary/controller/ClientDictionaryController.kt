package com.snurdy.backend.domain.dictionary.controller

import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.dictionary.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.domain.dictionary.service.ClientDictionaryService
import com.snurdy.backend.global.constants.DictionarySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ClientDictionaryController(
    private val clientDictionaryService: ClientDictionaryService
) : ClientDictionaryApi {
    override fun searchTerms(userId: UUID, query: String?, page: Int, size: Int): ApiResponse<TermSearchResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH,
            clientDictionaryService.searchTerms(userId, query, page, size)
        )

    override fun getTermDetail(userId: UUID, termId: Long): ApiResponse<ClientTermDetailResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH,
            clientDictionaryService.getTermDetail(userId, termId)
        )

    override fun getGlossaries(userId: UUID, category: String?): ApiResponse<ClientGlossaryListResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH,
            clientDictionaryService.getGlossaries(userId, category)
        )

    override fun getGlossaryTerms(
        userId: UUID,
        glossaryId: Long,
        filter: String,
        page: Int,
        size: Int
    ): ApiResponse<ClientGlossaryTermsResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH,
            clientDictionaryService.getGlossaryTerms(
                userId = userId,
                glossaryId = glossaryId,
                filter = filter,
                page = page,
                size = size
            )
        )
}
