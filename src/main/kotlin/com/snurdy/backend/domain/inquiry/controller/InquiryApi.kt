package com.snurdy.backend.domain.inquiry.controller

import com.snurdy.backend.domain.inquiry.docs.InquirySwaggerDescription
import com.snurdy.backend.domain.inquiry.dto.CreateInquiryRequest
import com.snurdy.backend.domain.inquiry.dto.InquiryDetailResponse
import com.snurdy.backend.domain.inquiry.dto.InquiryListResponse
import com.snurdy.backend.domain.inquiry.dto.UpdateInquiryRequest
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@Tag(
    name = InquirySwaggerDescription.INQUIRY_API_TAG_NAME,
    description = InquirySwaggerDescription.INQUIRY_API_TAG_DESCRIPTION
)
@RequestMapping("/v2/inquiries")
interface InquiryApi {

    @Operation(summary = InquirySwaggerDescription.CREATE_INQUIRY_SUMMARY)
    @ApiSuccessCode("INQUIRY_S001")
    @ApiErrorCodes("INQUIRY_E003")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun createInquiry(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: CreateInquiryRequest
    ): ApiResponse<InquiryDetailResponse>

    @Operation(summary = InquirySwaggerDescription.LIST_INQUIRIES_SUMMARY)
    @ApiSuccessCode("INQUIRY_S002")
    @GetMapping
    fun listInquiries(@CurrentUserId userId: UUID): ApiResponse<InquiryListResponse>

    @Operation(summary = InquirySwaggerDescription.GET_INQUIRY_SUMMARY)
    @ApiSuccessCode("INQUIRY_S003")
    @ApiErrorCodes("INQUIRY_E001")
    @GetMapping("/{inquiryId}")
    fun getInquiry(
        @CurrentUserId userId: UUID,
        @PathVariable inquiryId: Long
    ): ApiResponse<InquiryDetailResponse>

    @Operation(summary = InquirySwaggerDescription.UPDATE_INQUIRY_SUMMARY)
    @ApiSuccessCode("INQUIRY_S004")
    @ApiErrorCodes("INQUIRY_E001", "INQUIRY_E002", "INQUIRY_E003")
    @PutMapping("/{inquiryId}")
    fun updateInquiry(
        @CurrentUserId userId: UUID,
        @PathVariable inquiryId: Long,
        @RequestBody @Valid request: UpdateInquiryRequest
    ): ApiResponse<InquiryDetailResponse>

    @Operation(summary = InquirySwaggerDescription.DELETE_INQUIRY_SUMMARY)
    @ApiSuccessCode("INQUIRY_S005")
    @ApiErrorCodes("INQUIRY_E001", "INQUIRY_E002")
    @DeleteMapping("/{inquiryId}")
    fun deleteInquiry(
        @CurrentUserId userId: UUID,
        @PathVariable inquiryId: Long
    ): ApiResponse<Unit>
}