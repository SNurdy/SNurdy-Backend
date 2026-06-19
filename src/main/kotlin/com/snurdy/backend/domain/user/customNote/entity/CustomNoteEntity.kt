package com.snurdy.backend.domain.user.customNote.entity

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
    name = "custom_notes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_custom_note_owner_title", columnNames = ["owner_id", "title"]),
        UniqueConstraint(name = "uk_custom_note_share_code", columnNames = ["share_code"])
    ]
)
class CustomNoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L,

    @Column(name = "owner_id", columnDefinition = "BINARY(16)", nullable = false)
    val ownerId: UUID,

    @Column(name = "title", nullable = false, length = 14)
    var title: String,

    // 공유 코드. null = 공유 비활성. 비활성화 시 코드 폐기 (감사 추적 없음)
    @Column(name = "share_code", length = 12)
    var shareCode: String? = null
) : BaseTimeEntity() {

    fun rename(newTitle: String) {
        this.title = newTitle
    }

    fun isShareActive(): Boolean = shareCode != null

    fun issueShareCode(code: String) {
        this.shareCode = code
    }

    fun revokeShare() {
        this.shareCode = null
    }
}
