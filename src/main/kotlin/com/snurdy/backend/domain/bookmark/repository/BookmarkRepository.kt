package com.snurdy.backend.domain.bookmark.repository

import com.snurdy.backend.domain.bookmark.entity.BookmarkEntity
import com.snurdy.backend.domain.bookmark.entity.BookmarkId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BookmarkRepository : JpaRepository<BookmarkEntity, BookmarkId> {
    fun existsByIdUserId(userId: UUID): Boolean

    fun countByIdUserId(userId: UUID): Long

    fun findAllByIdUserIdOrderByCreatedAtDesc(userId: UUID): List<BookmarkEntity>

    fun findAllByIdUserIdAndIdTermIdIn(userId: UUID, termIds: List<Long>): List<BookmarkEntity>

    fun findAllByIdUserIdAndIdTermId(userId: UUID, termId: Long): List<BookmarkEntity>

    fun deleteByIdUserIdAndIdTermId(userId: UUID, termId: Long): Long
}
