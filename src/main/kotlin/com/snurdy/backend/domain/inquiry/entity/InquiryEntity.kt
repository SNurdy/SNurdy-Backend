package com.snurdy.backend.domain.inquiry.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inquiries")
class InquiryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L,

    // 사용자 물리 삭제 시 SET NULL 로 문의를 보존하므로 nullable
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    val userId: UUID?,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    var category: InquiryCategory,

    @Column(name = "content", nullable = false, length = MAX_CONTENT_LENGTH)
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: InquiryStatus = InquiryStatus.WAITING,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
) : BaseTimeEntity() {

    // 사용자 수정과 관리자 답변이 동시에 커밋되면 한쪽을 실패시키기 위한 낙관적 락
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L
        protected set

    fun isDeleted(): Boolean = deletedAt != null

    fun isAnswered(): Boolean = status == InquiryStatus.ANSWERED

    fun hasSameCategoryAndContent(category: InquiryCategory, content: String): Boolean =
        this.category == category && this.content == content

    fun updateCategoryAndContent(category: InquiryCategory, content: String) {
        this.category = category
        this.content = content
    }

    fun markAnswered() {
        this.status = InquiryStatus.ANSWERED
    }

    fun softDelete(at: Instant) {
        this.deletedAt = at
    }

    companion object {
        const val MAX_CONTENT_LENGTH = 1000
    }
}