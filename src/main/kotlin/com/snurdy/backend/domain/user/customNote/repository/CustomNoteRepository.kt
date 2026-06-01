package com.snurdy.backend.domain.user.customNote.repository

import com.snurdy.backend.domain.user.customNote.entity.CustomNoteEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CustomNoteRepository : JpaRepository<CustomNoteEntity, Long> {

    fun countByOwnerId(ownerId: UUID): Long

    fun existsByOwnerIdAndTitle(ownerId: UUID, title: String): Boolean

    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<CustomNoteEntity>
}
