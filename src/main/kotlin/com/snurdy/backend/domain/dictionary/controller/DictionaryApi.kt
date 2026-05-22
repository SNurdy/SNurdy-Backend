package com.snurdy.backend.domain.dictionary.controller

import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.dictionary.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
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
import java.util.UUID

@Validated
@Tag(name = "Dictionary", description = "사전 API — 단어 검색/상세/단어장 목록 + 본인 컨텍스트 (북마크/메모/진행률) 결합")
@RequestMapping("/v1/dictionary")
interface DictionaryApi {

    @Operation(summary = "용어 검색")
    @ApiSuccessCode("DICTIONARY_S002")
    @GetMapping("/terms")
    fun searchTerms(
        @CurrentUserId userId: UUID,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<TermSearchResponse>

    @Operation(summary = "용어 상세 조회 (북마크/메모 포함)")
    @ApiSuccessCode("DICTIONARY_S003")
    @ApiErrorCodes("DICTIONARY_E002")
    @GetMapping("/terms/{termId}")
    fun getTermDetail(
        @CurrentUserId userId: UUID,
        @PathVariable termId: Long
    ): ApiResponse<ClientTermDetailResponse>

    @Operation(summary = "어근어미 키워드로 용어 검색")
    @ApiSuccessCode("DICTIONARY_S006")
    @GetMapping("/terms/search/morpheme")
    fun searchTermsByMorpheme(
        @Parameter(description = "어근어미 키워드 (최소 2자)", example = "hepat")
        @RequestParam @Size(
            min = 2, max = 50,
            message = "{com.snurdy.backend.global.constants.DictionaryError.INVALID_SEARCH_KEYWORD}"
        ) keyword: String
    ): ApiResponse<MorphemeSearchResponse>

    @Operation(summary = "용어장 목록 조회 (DEFAULT 단어장만)")
    @ApiSuccessCode("DICTIONARY_S004")
    @GetMapping("/glossaries")
    fun getGlossaries(
        @CurrentUserId userId: UUID,
        @Parameter(description = "카테고리 필터")
        @RequestParam(required = false) category: String?
    ): ApiResponse<ClientGlossaryListResponse>

    @Operation(summary = "용어장 용어 목록 조회")
    @ApiSuccessCode("DICTIONARY_S005")
    @ApiErrorCodes("DICTIONARY_E001", "DICTIONARY_E005")
    @GetMapping("/glossaries/{glossaryId}/terms")
    fun getGlossaryTerms(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestParam(defaultValue = "all") filter: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<ClientGlossaryTermsResponse>
}
