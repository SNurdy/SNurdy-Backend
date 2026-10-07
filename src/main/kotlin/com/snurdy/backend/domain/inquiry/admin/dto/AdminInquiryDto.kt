package com.snurdy.backend.domain.inquiry.admin.dto

import com.snurdy.backend.domain.inquiry.docs.InquirySwaggerDescription
import com.snurdy.backend.domain.inquiry.docs.InquirySwaggerExample
import com.snurdy.backend.domain.inquiry.entity.InquiryAnswerEntity
import com.snurdy.backend.global.validation.CodePointSize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Schema(description = InquirySwaggerDescription.CREATE_INQUIRY_ANSWER_REQUEST)
data class CreateInquiryAnswerRequest(
    @field:NotBlank(message = "답변 내용이 비어 있을 수 없습니다")
    @field:CodePointSize(max = InquiryAnswerEntity.MAX_CONTENT_LENGTH, message = "답변 내용은 최대 2000자까지 입력할 수 있습니다")
    @field:Schema(description = InquirySwaggerDescription.INQUIRY_ANSWER_REQUEST_CONTENT, example = InquirySwaggerExample.INQUIRY_ANSWER_CONTENT)
    val content: String
)

fun CreateInquiryAnswerRequest.toEntity(inquiryId: Long, adminUserId: UUID): InquiryAnswerEntity =
    InquiryAnswerEntity(
        inquiryId = inquiryId,
        adminUserId = adminUserId,
        content = content.trim()
    )