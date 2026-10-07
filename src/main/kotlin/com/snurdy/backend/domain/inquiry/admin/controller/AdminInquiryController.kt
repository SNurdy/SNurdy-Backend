package com.snurdy.backend.domain.inquiry.admin.controller

import com.snurdy.backend.domain.inquiry.admin.dto.CreateInquiryAnswerRequest
import com.snurdy.backend.domain.inquiry.admin.service.AdminInquiryService
import com.snurdy.backend.domain.inquiry.dto.InquiryAnswerResponse
import com.snurdy.backend.global.constants.InquirySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AdminInquiryController(
    private val adminInquiryService: AdminInquiryService
) : AdminInquiryApi {

    override fun createInquiryAnswer(
        adminUserId: UUID,
        inquiryId: Long,
        request: CreateInquiryAnswerRequest
    ): ApiResponse<InquiryAnswerResponse> =
        ApiResponse.success(
            InquirySuccess.CREATE_INQUIRY_ANSWER,
            adminInquiryService.createInquiryAnswer(adminUserId, inquiryId, request)
        )
}