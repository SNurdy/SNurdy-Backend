package com.snurdy.backend.domain.admin.dictionary.controller

import com.snurdy.backend.domain.admin.dictionary.dto.*
import com.snurdy.backend.domain.admin.dictionary.service.AdminGlossaryQueryService
import com.snurdy.backend.domain.admin.dictionary.service.AdminMorphemeQueryService
import com.snurdy.backend.domain.admin.dictionary.service.AdminTermQueryService
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminDictionaryController(
    private val glossaryQueryService: AdminGlossaryQueryService,
    private val termQueryService: AdminTermQueryService,
    private val morphemeQueryService: AdminMorphemeQueryService
) : AdminDictionaryApi {

    override fun getGlossaries(keyword: String?, page: Int, size: Int): ApiResponse<GlossaryPageResponse> =
        ApiResponse.success(CommonSuccess.OK, glossaryQueryService.getGlossaries(keyword, page, size))

    override fun getTerms(keyword: String?, page: Int, size: Int): ApiResponse<TermPageResponse> =
        ApiResponse.success(CommonSuccess.OK, termQueryService.getTerms(keyword, page, size))

    override fun getTerm(termId: Long): ApiResponse<TermDetailResponse> =
        ApiResponse.success(CommonSuccess.OK, termQueryService.getTerm(termId))

    override fun getMorphemes(keyword: String?, page: Int, size: Int): ApiResponse<MorphemePageResponse> =
        ApiResponse.success(CommonSuccess.OK, morphemeQueryService.getMorphemes(keyword, page, size))

    override fun getMorphemeVariants(
        keyword: String?,
        morphemeId: Long?,
        page: Int,
        size: Int
    ): ApiResponse<MorphemeVariantPageResponse> =
        ApiResponse.success(CommonSuccess.OK, morphemeQueryService.getMorphemeVariants(keyword, morphemeId, page, size))
}
