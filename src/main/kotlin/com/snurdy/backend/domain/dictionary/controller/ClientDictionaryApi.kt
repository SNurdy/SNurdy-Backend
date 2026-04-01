package com.snurdy.backend.domain.dictionary.controller

import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.dictionary.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@Validated
@Tag(name = "Learning Dictionary", description = "iOS 호환 학습 사전 API")
interface ClientDictionaryApi {

    @Operation(summary = "용어 검색")
    @GetMapping("/v1/dictionary/terms")
    fun searchTerms(
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<TermSearchResponse>

    @Operation(summary = "용어 상세 조회")
    @GetMapping("/v1/dictionary/terms/{termId}")
    fun getTermDetail(
        @PathVariable termId: Long
    ): ApiResponse<ClientTermDetailResponse>

    @Operation(summary = "용어장 목록 조회")
    @GetMapping("/v1/dictionary/glossaries")
    fun getGlossaries(
        @Parameter(description = "카테고리 필터")
        @RequestParam(required = false) category: String?
    ): ApiResponse<ClientGlossaryListResponse>

    @Operation(summary = "용어장 용어 목록 조회")
    @GetMapping("/v1/dictionary/glossaries/{glossaryId}/terms")
    fun getGlossaryTerms(
        @PathVariable glossaryId: Long,
        @RequestParam(defaultValue = "all") filter: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<ClientGlossaryTermsResponse>
}
