package com.snurdy.backend.domain.bookmark.repository

import com.snurdy.backend.domain.bookmark.entity.BookmarkEntity
import com.snurdy.backend.domain.bookmark.entity.BookmarkId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface BookmarkRepository : JpaRepository<BookmarkEntity, BookmarkId> {
    fun existsByIdUserId(userId: UUID): Boolean

    fun countByIdUserId(userId: UUID): Long

    // term JOIN FETCH로 N+1 방지. 페이징 전환 시 countQuery는
    // "SELECT COUNT(b) FROM BookmarkEntity b WHERE b.id.userId = :userId" 를 별도로 지정할 것.
    @Query(
        """
        SELECT b
        FROM BookmarkEntity b
        JOIN FETCH b.term
        WHERE b.id.userId = :userId
        ORDER BY b.createdAt DESC
        """
    )
    fun findAllWithTermByUserId(@Param("userId") userId: UUID): List<BookmarkEntity>

    fun findAllByIdUserIdAndIdTermIdIn(userId: UUID, termIds: List<Long>): List<BookmarkEntity>

    fun findAllByIdUserIdAndIdTermId(userId: UUID, termId: Long): List<BookmarkEntity>

    fun deleteByIdUserIdAndIdTermId(userId: UUID, termId: Long): Long
}
