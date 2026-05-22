package com.snurdy.backend.domain.dictionary.controller

import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.domain.dictionary.service.TermSearchService
import com.snurdy.backend.global.constants.DictionarySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class MorphemeSearchController(
    private val termSearchService: TermSearchService
) : MorphemeSearchApi {

    override fun searchTermsByMorpheme(keyword: String): ApiResponse<MorphemeSearchResponse> =
        ApiResponse.success(DictionarySuccess.SEARCH_BY_MORPHEME, termSearchService.searchTermsByMorpheme(keyword))
}
