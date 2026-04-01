package com.snurdy.backend.domain.admin.dictionary.controller

import com.snurdy.backend.domain.admin.dictionary.dto.*
import com.snurdy.backend.domain.admin.dictionary.service.AdminDictionaryQueryService
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminDictionaryController(
    private val adminDictionaryQueryService: AdminDictionaryQueryService
) : AdminDictionaryApi {

    override fun getGlossaries(keyword: String?, page: Int, size: Int): ApiResponse<GlossaryPageResponse> =
        ApiResponse.success(CommonSuccess.OK, adminDictionaryQueryService.getGlossaries(keyword, page, size))

    override fun getTerms(keyword: String?, page: Int, size: Int): ApiResponse<TermPageResponse> =
        ApiResponse.success(CommonSuccess.OK, adminDictionaryQueryService.getTerms(keyword, page, size))

    override fun getTerm(termId: Long): ApiResponse<TermDetailResponse> =
        ApiResponse.success(CommonSuccess.OK, adminDictionaryQueryService.getTerm(termId))

    override fun getMorphemes(keyword: String?, page: Int, size: Int): ApiResponse<MorphemePageResponse> =
        ApiResponse.success(CommonSuccess.OK, adminDictionaryQueryService.getMorphemes(keyword, page, size))

    override fun getMorphemeVariants(
        keyword: String?,
        morphemeId: Long?,
        page: Int,
        size: Int
    ): ApiResponse<MorphemeVariantPageResponse> =
        ApiResponse.success(CommonSuccess.OK, adminDictionaryQueryService.getMorphemeVariants(keyword, morphemeId, page, size))
}
