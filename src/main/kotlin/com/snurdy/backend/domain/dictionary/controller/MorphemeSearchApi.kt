package com.snurdy.backend.domain.dictionary.controller

import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Validated
@Tag(name = "Dictionary Morpheme", description = "사전 어근어미 기반 용어 검색 API")
@RequestMapping("/v1/dictionary")
interface MorphemeSearchApi {

    @Operation(summary = "어근어미 키워드로 용어 검색")
    @GetMapping("/terms/search/morpheme")
    fun searchTermsByMorpheme(
        @Parameter(description = "어근어미 키워드 (최소 2자)", example = "hepat")
        @RequestParam @Size(
            min = 2, max = 50,
            message = "{com.snurdy.backend.global.constants.DictionaryError.INVALID_SEARCH_KEYWORD}"
        ) keyword: String
    ): ApiResponse<MorphemeSearchResponse>
}
