package com.snurdy.backend.domain.inquiry.controller

import com.snurdy.backend.domain.inquiry.dto.CreateInquiryRequest
import com.snurdy.backend.domain.inquiry.dto.InquiryDetailResponse
import com.snurdy.backend.domain.inquiry.dto.InquiryListResponse
import com.snurdy.backend.domain.inquiry.dto.UpdateInquiryRequest
import com.snurdy.backend.domain.inquiry.service.InquiryService
import com.snurdy.backend.global.constants.InquirySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class InquiryController(
    private val inquiryService: InquiryService
) : InquiryApi {

    override fun createInquiry(userId: UUID, request: CreateInquiryRequest): ApiResponse<InquiryDetailResponse> =
        ApiResponse.success(InquirySuccess.CREATE_INQUIRY, inquiryService.createInquiry(userId, request))

    override fun listInquiries(userId: UUID): ApiResponse<InquiryListResponse> =
        ApiResponse.success(InquirySuccess.LIST_INQUIRIES, inquiryService.listInquiries(userId))

    override fun getInquiry(userId: UUID, inquiryId: Long): ApiResponse<InquiryDetailResponse> =
        ApiResponse.success(InquirySuccess.GET_INQUIRY, inquiryService.getInquiry(userId, inquiryId))

    override fun updateInquiry(
        userId: UUID,
        inquiryId: Long,
        request: UpdateInquiryRequest
    ): ApiResponse<InquiryDetailResponse> =
        ApiResponse.success(InquirySuccess.UPDATE_INQUIRY, inquiryService.updateInquiry(userId, inquiryId, request))

    override fun deleteInquiry(userId: UUID, inquiryId: Long): ApiResponse<Unit> {
        inquiryService.deleteInquiry(userId, inquiryId)
        return ApiResponse.success(InquirySuccess.DELETE_INQUIRY)
    }
}