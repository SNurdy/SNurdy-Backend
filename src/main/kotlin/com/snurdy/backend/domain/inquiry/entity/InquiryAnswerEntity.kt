package com.snurdy.backend.domain.inquiry.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "inquiry_answers",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_inquiry_answers_inquiry", columnNames = ["inquiry_id"])
    ]
)
class InquiryAnswerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L,

    @Column(name = "inquiry_id", nullable = false)
    val inquiryId: Long,

    @Column(name = "admin_user_id", columnDefinition = "BINARY(16)", nullable = false)
    val adminUserId: UUID,

    @Column(name = "content", nullable = false, length = MAX_CONTENT_LENGTH)
    var content: String
) : BaseTimeEntity() {

    fun updateContent(newContent: String) {
        this.content = newContent
    }

    companion object {
        const val MAX_CONTENT_LENGTH = 2000
    }
}