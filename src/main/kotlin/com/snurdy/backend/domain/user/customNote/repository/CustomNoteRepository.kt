package com.snurdy.backend.domain.user.customNote.repository

import com.snurdy.backend.domain.user.customNote.entity.CustomNoteEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CustomNoteRepository : JpaRepository<CustomNoteEntity, Long> {

    fun countByOwnerId(ownerId: UUID): Long

    fun existsByOwnerIdAndTitle(ownerId: UUID, title: String): Boolean

    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<CustomNoteEntity>

    // 공유 코드로 노트 조회. null 인 row 는 자동 매칭 안 됨
    fun findByShareCode(shareCode: String): CustomNoteEntity?

    fun existsByShareCode(shareCode: String): Boolean
}
