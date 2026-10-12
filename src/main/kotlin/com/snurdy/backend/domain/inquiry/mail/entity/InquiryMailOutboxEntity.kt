package com.snurdy.backend.domain.inquiry.mail.entity

import com.snurdy.backend.domain.inquiry.entity.InquiryCategory
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "inquiry_mail_outbox")
class InquiryMailOutboxEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L,

    @Column(name = "inquiry_id", nullable = false)
    val inquiryId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "mail_type", nullable = false, length = 20)
    val mailType: InquiryMailType,

    // 작성 직후 수정되어도 메일마다 해당 시점 내용이 가도록 스냅샷 보관
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    val category: InquiryCategory,

    @Column(name = "content", nullable = false, length = 1000)
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: InquiryMailStatus = InquiryMailStatus.PENDING,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    var lastError: String? = null,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Column(name = "sent_at")
    var sentAt: Instant? = null
) : BaseTimeEntity() {

    fun isSent(): Boolean = status == InquiryMailStatus.SENT

    companion object {
        const val MAX_ERROR_LENGTH = 500
    }
}