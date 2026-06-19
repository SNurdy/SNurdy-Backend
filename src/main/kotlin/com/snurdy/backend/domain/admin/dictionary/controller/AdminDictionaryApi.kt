package com.snurdy.backend.domain.admin.dictionary.controller

import com.snurdy.backend.domain.admin.dictionary.dto.*
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Validated
@Tag(name = "Admin Dictionary", description = "관리자 사전 데이터 조회 API")
@RequestMapping("/v1/admin/dictionary")
interface AdminDictionaryApi {

    @Operation(summary = "용어집 목록 조회")
    @ApiSuccessCode("ADMIN_S030")
    @GetMapping("/glossaries")
    fun getGlossaries(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<GlossaryPageResponse>

    @Operation(summary = "용어 목록 조회")
    @ApiSuccessCode("ADMIN_S031")
    @GetMapping("/terms")
    fun getTerms(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<TermPageResponse>

    @Operation(summary = "용어 상세 조회")
    @ApiSuccessCode("ADMIN_S032")
    @ApiErrorCodes("DICTIONARY_E002")
    @GetMapping("/terms/{termId}")
    fun getTerm(
        @PathVariable termId: Long
    ): ApiResponse<TermDetailResponse>

    @Operation(summary = "어근어미 목록 조회")
    @ApiSuccessCode("ADMIN_S033")
    @GetMapping("/morphemes")
    fun getMorphemes(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<MorphemePageResponse>

    @Operation(summary = "어근어미 variant 목록 조회")
    @ApiSuccessCode("ADMIN_S034")
    @GetMapping("/morpheme-variants")
    fun getMorphemeVariants(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) morphemeId: Long?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<MorphemeVariantPageResponse>
}
