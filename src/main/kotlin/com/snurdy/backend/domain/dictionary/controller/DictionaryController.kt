package com.snurdy.backend.domain.dictionary.controller

import com.snurdy.backend.domain.dictionary.dto.GlossaryListResponse
import com.snurdy.backend.domain.dictionary.dto.GlossaryTermsResponse
import com.snurdy.backend.domain.dictionary.dto.TermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeListResponse
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.domain.dictionary.service.GlossaryListingService
import com.snurdy.backend.domain.dictionary.service.MorphemeListingService
import com.snurdy.backend.domain.dictionary.service.TermDetailService
import com.snurdy.backend.domain.dictionary.service.TermSearchService
import com.snurdy.backend.global.constants.DictionarySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DictionaryController(
    private val termSearchService: TermSearchService,
    private val termDetailService: TermDetailService,
    private val glossaryListingService: GlossaryListingService,
    private val morphemeListingService: MorphemeListingService
) : DictionaryApi {

    override fun searchTerms(
        userId: UUID,
        query: String?,
        category: String?,
        glossaryId: Long?,
        page: Int,
        size: Int
    ): ApiResponse<TermSearchResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH_TERMS,
            termSearchService.searchTerms(userId, query, category, glossaryId, morphemeId = null, page, size)
        )

    override fun getTermDetail(userId: UUID, termId: Long): ApiResponse<TermDetailResponse> =
        ApiResponse.success(
            DictionarySuccess.GET_TERM_DETAIL,
            termDetailService.getTermDetail(userId, termId)
        )

    override fun searchTermsByMorpheme(keyword: String): ApiResponse<MorphemeSearchResponse> =
        ApiResponse.success(
            DictionarySuccess.SEARCH_BY_MORPHEME,
            termSearchService.searchTermsByMorpheme(keyword)
        )

    override fun listMorphemes(
        query: String?,
        page: Int,
        size: Int
    ): ApiResponse<MorphemeListResponse> =
        ApiResponse.success(
            DictionarySuccess.LIST_MORPHEMES,
            morphemeListingService.listMorphemes(query, page, size)
        )

    override fun listTermsByMorpheme(
        userId: UUID,
        morphemeId: Long,
        category: String?,
        query: String?,
        page: Int,
        size: Int
    ): ApiResponse<TermSearchResponse> =
        ApiResponse.success(
            DictionarySuccess.LIST_TERMS_BY_MORPHEME,
            morphemeListingService.listTermsByMorpheme(userId, morphemeId, category, query, page, size)
        )

    override fun getGlossaries(userId: UUID, category: String?): ApiResponse<GlossaryListResponse> =
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
    ): ApiResponse<GlossaryTermsResponse> =
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
