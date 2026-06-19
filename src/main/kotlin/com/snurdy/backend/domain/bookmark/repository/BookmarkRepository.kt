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

    // 학습 카드용 batch lookup. 현재 학습 중인 단어장 안에서 북마크된 termId 목록만 반환
    @Query(
        value = """
        SELECT b.id.termId
        FROM BookmarkEntity b
        WHERE b.id.userId = :userId
          AND b.id.glossaryId = :glossaryId
          AND b.id.termId IN :termIds
        """
    )
    fun findBookmarkedTermIds(
        @Param("userId") userId: UUID,
        @Param("glossaryId") glossaryId: Long,
        @Param("termIds") termIds: Collection<Long>
    ): List<Long>

    // 오답노트 카드용 batch lookup
    // (glossaryId, termId) 쌍 단위 조회. 단어가 단어별로 다른 sourceGlossaryId 일 수 있음 (by-period)
    @Query(
        value = """
        SELECT b.id.glossaryId AS glossaryId, b.id.termId AS termId
        FROM BookmarkEntity b
        WHERE b.id.userId = :userId
          AND b.id.glossaryId IN :glossaryIds
          AND b.id.termId IN :termIds
        """
    )
    fun findBookmarkedPairs(
        @Param("userId") userId: UUID,
        @Param("glossaryIds") glossaryIds: Collection<Long>,
        @Param("termIds") termIds: Collection<Long>
    ): List<BookmarkPairProjection>
}

interface BookmarkPairProjection {
    val glossaryId: Long
    val termId: Long
}
