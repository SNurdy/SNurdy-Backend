package com.snurdy.backend.domain.user.customNote.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "custom_note_share_codes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_share_code", columnNames = ["share_code"])
    ]
)
class CustomNoteShareCodeEntity(
    @Id
    @Column(name = "note_id", nullable = false)
    val noteId: Long,

    @Column(name = "share_code", nullable = false, length = 12)
    var shareCode: String,

    @Column(name = "issued_at", nullable = false)
    var issuedAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
) {

    // 활성 코드 여부
    fun isActive(): Boolean = revokedAt == null

    // 비활성화
    fun revoke(at: Instant) {
        this.revokedAt = at
    }

    // revoked 상태에서 같은 row 재발급. 새 코드 + 시각 갱신
    fun reissue(newCode: String, at: Instant) {
        this.shareCode = newCode
        this.issuedAt = at
        this.revokedAt = null
    }
}
