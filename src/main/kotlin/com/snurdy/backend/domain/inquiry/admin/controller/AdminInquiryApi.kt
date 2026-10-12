package com.snurdy.backend.domain.inquiry.admin.controller

import com.snurdy.backend.domain.inquiry.admin.dto.CreateInquiryAnswerRequest
import com.snurdy.backend.domain.inquiry.docs.InquirySwaggerDescription
import com.snurdy.backend.domain.inquiry.dto.InquiryAnswerResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@Tag(
    name = InquirySwaggerDescription.ADMIN_INQUIRY_API_TAG_NAME,
    description = InquirySwaggerDescription.ADMIN_INQUIRY_API_TAG_DESCRIPTION
)
@RequestMapping("/v2/admin/inquiries")
interface AdminInquiryApi {

    @Operation(summary = InquirySwaggerDescription.CREATE_INQUIRY_ANSWER_SUMMARY)
    @ApiSuccessCode("INQUIRY_S008")
    @ApiErrorCodes("INQUIRY_E001", "INQUIRY_E005")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{inquiryId}/answer")
    fun createInquiryAnswer(
        @CurrentUserId adminUserId: UUID,
        @PathVariable inquiryId: Long,
        @RequestBody @Valid request: CreateInquiryAnswerRequest
    ): ApiResponse<InquiryAnswerResponse>
}