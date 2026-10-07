package com.snurdy.backend.domain.inquiry.mail.sender

import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailOutboxEntity
import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailType
import com.snurdy.backend.global.constants.APP_ZONE_ID
import com.snurdy.backend.global.extension.toSingleLine
import com.snurdy.backend.global.extension.truncateByCodePoints
import java.time.format.DateTimeFormatter
import java.util.UUID

data class InquiryMailContent(
    val subject: String,
    val body: String
)

fun InquiryMailOutboxEntity.toMailContent(authorId: UUID?, subjectPrefix: String): InquiryMailContent {
    val updatedTag = if (mailType == InquiryMailType.UPDATED) "[${mailType.label}]" else ""
    val subjectPreview = content.toSingleLine().truncateByCodePoints(SUBJECT_PREVIEW_LENGTH)
    val receivedAt = MAIL_TIME_FORMATTER.format(checkNotNull(createdAt))

    return InquiryMailContent(
        subject = "$subjectPrefix[SNurdy 문의]$updatedTag[${category.label}] #$inquiryId $subjectPreview",
        body = listOf(
            "문의 번호: #$inquiryId",
            "구분: ${mailType.label}",
            "유형: ${category.label}",
            "접수 시각: $receivedAt (KST)",
            "작성자: ${authorId ?: "탈퇴한 사용자"}",
            "",
            content
        ).joinToString("\n")
    )
}

private const val SUBJECT_PREVIEW_LENGTH = 30

private val MAIL_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(APP_ZONE_ID)