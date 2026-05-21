package com.snurdy.backend.domain.bookmark.repository

import com.snurdy.backend.domain.bookmark.entity.BookmarkEntity
import com.snurdy.backend.domain.bookmark.entity.BookmarkId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface BookmarkRepository : JpaRepository<BookmarkEntity, BookmarkId> {
    fun existsByIdUserId(userId: UUID): Boolean

    fun countByIdUserId(userId: UUID): Long

    @Query(
        value = """
            SELECT b
            FROM BookmarkEntity b
            JOIN FETCH b.term
            WHERE b.id.userId = :userId
            ORDER BY b.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(b)
            FROM BookmarkEntity b
            WHERE b.id.userId = :userId
        """
    )
    fun findAllWithTermByUserId(@Param("userId") userId: UUID, pageable: Pageable): Page<BookmarkEntity>

    fun findAllByIdUserIdAndIdTermIdIn(userId: UUID, termIds: List<Long>): List<BookmarkEntity>

    fun findAllByIdUserIdAndIdTermId(userId: UUID, termId: Long): List<BookmarkEntity>

    fun deleteByIdUserIdAndIdGlossaryId(userId: UUID, glossaryId: Long)

    fun deleteByIdUserIdAndIdGlossaryIdAndIdTermId(userId: UUID, glossaryId: Long, termId: Long)
}
