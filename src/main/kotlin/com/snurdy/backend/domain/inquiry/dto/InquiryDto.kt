package com.snurdy.backend.domain.inquiry.dto

import com.snurdy.backend.domain.inquiry.docs.InquirySwaggerDescription
import com.snurdy.backend.domain.inquiry.docs.InquirySwaggerExample
import com.snurdy.backend.domain.inquiry.entity.InquiryAnswerEntity
import com.snurdy.backend.domain.inquiry.entity.InquiryCategory
import com.snurdy.backend.domain.inquiry.entity.InquiryEntity
import com.snurdy.backend.domain.inquiry.entity.InquiryStatus
import com.snurdy.backend.global.extension.toSingleLine
import com.snurdy.backend.global.extension.truncateByCodePoints
import com.snurdy.backend.global.validation.CodePointSize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

private const val CONTENT_PREVIEW_LENGTH = 20

@Schema(description = InquirySwaggerDescription.CREATE_INQUIRY_REQUEST)
data class CreateInquiryRequest(
    @field:Schema(description = InquirySwaggerDescription.INQUIRY_CATEGORY, example = InquirySwaggerExample.INQUIRY_CATEGORY)
    val category: InquiryCategory,

    @field:NotBlank(message = "문의 내용이 비어 있을 수 없습니다")
    @field:CodePointSize(max = InquiryEntity.MAX_CONTENT_LENGTH, message = "문의 내용은 최대 1000자까지 입력할 수 있습니다")
    @field:Schema(description = InquirySwaggerDescription.INQUIRY_REQUEST_CONTENT, example = InquirySwaggerExample.INQUIRY_CONTENT)
    val content: String
)

@Schema(description = InquirySwaggerDescription.UPDATE_INQUIRY_REQUEST)
data class UpdateInquiryRequest(
    @field:Schema(description = InquirySwaggerDescription.INQUIRY_CATEGORY, example = InquirySwaggerExample.INQUIRY_CATEGORY)
    val category: InquiryCategory,

    @field:NotBlank(message = "문의 내용이 비어 있을 수 없습니다")
    @field:CodePointSize(max = InquiryEntity.MAX_CONTENT_LENGTH, message = "문의 내용은 최대 1000자까지 입력할 수 있습니다")
    @field:Schema(description = InquirySwaggerDescription.INQUIRY_REQUEST_CONTENT, example = InquirySwaggerExample.INQUIRY_CONTENT)
    val content: String
)

@Schema(description = InquirySwaggerDescription.INQUIRY_ANSWER)
data class InquiryAnswerResponse(
    @field:Schema(description = InquirySwaggerDescription.INQUIRY_ANSWER_CONTENT, example = InquirySwaggerExample.INQUIRY_ANSWER_CONTENT)
    val content: String,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_ANSWERED_AT, example = InquirySwaggerExample.INQUIRY_ANSWERED_AT)
    val answeredAt: Instant?
)

fun InquiryAnswerEntity.toResponse(): InquiryAnswerResponse =
    InquiryAnswerResponse(content = content, answeredAt = createdAt)

@Schema(description = InquirySwaggerDescription.INQUIRY_SUMMARY)
data class InquirySummaryResponse(
    @field:Schema(description = InquirySwaggerDescription.INQUIRY_ID, example = InquirySwaggerExample.INQUIRY_ID)
    val inquiryId: Long,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_CATEGORY, example = InquirySwaggerExample.INQUIRY_CATEGORY)
    val category: InquiryCategory,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_CONTENT_PREVIEW, example = InquirySwaggerExample.INQUIRY_CONTENT_PREVIEW)
    val contentPreview: String,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_STATUS, example = InquirySwaggerExample.INQUIRY_STATUS)
    val status: InquiryStatus,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_CREATED_AT, example = InquirySwaggerExample.INQUIRY_CREATED_AT)
    val createdAt: Instant?
)

@Schema(description = InquirySwaggerDescription.INQUIRY_LIST_RESPONSE)
data class InquiryListResponse(
    val inquiries: List<InquirySummaryResponse>
)

fun InquiryEntity.toSummary(): InquirySummaryResponse =
    InquirySummaryResponse(
        inquiryId = id,
        category = category,
        contentPreview = content.toSingleLine().truncateByCodePoints(CONTENT_PREVIEW_LENGTH),
        status = status,
        createdAt = createdAt
    )

@Schema(description = InquirySwaggerDescription.INQUIRY_DETAIL)
data class InquiryDetailResponse(
    @field:Schema(description = InquirySwaggerDescription.INQUIRY_ID, example = InquirySwaggerExample.INQUIRY_ID)
    val inquiryId: Long,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_CATEGORY, example = InquirySwaggerExample.INQUIRY_CATEGORY)
    val category: InquiryCategory,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_CONTENT, example = InquirySwaggerExample.INQUIRY_CONTENT)
    val content: String,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_STATUS, example = InquirySwaggerExample.INQUIRY_STATUS)
    val status: InquiryStatus,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_EDITABLE, example = InquirySwaggerExample.INQUIRY_EDITABLE)
    val editable: Boolean,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_ANSWER)
    val answer: InquiryAnswerResponse?,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_CREATED_AT, example = InquirySwaggerExample.INQUIRY_CREATED_AT)
    val createdAt: Instant?,

    @field:Schema(description = InquirySwaggerDescription.INQUIRY_UPDATED_AT, example = InquirySwaggerExample.INQUIRY_UPDATED_AT)
    val updatedAt: Instant?
)

fun InquiryEntity.toDetail(answer: InquiryAnswerEntity?): InquiryDetailResponse =
    InquiryDetailResponse(
        inquiryId = id,
        category = category,
        content = content,
        status = status,
        editable = !isAnswered(),
        answer = answer?.toResponse(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun CreateInquiryRequest.toEntity(userId: UUID): InquiryEntity =
    InquiryEntity(
        userId = userId,
        category = category,
        content = content.trim()
    )