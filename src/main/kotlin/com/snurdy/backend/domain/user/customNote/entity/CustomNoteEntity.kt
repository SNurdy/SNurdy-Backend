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
        UniqueConstraint(name = "uk_custom_note_owner_title", columnNames = ["owner_id", "title"])
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
    var title: String
) : BaseTimeEntity() {

    fun rename(newTitle: String) {
        this.title = newTitle
    }
}
