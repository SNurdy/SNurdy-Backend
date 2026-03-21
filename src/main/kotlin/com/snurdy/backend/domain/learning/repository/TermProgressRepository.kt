package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface TermProgressRepository : JpaRepository<TermProgressEntity, UUID> {
    fun existsByUserId(userId: UUID): Boolean

    fun countByUserId(userId: UUID): Long

    fun findByUserIdAndGlossaryIdAndTermId(userId: UUID, glossaryId: Long, termId: Long): TermProgressEntity?

    fun findByUserIdAndGlossaryIdAndTermIdIn(
        userId: UUID,
        glossaryId: Long,
        termIds: List<Long>
    ): List<TermProgressEntity>

    @Query(
        """
        SELECT tp
        FROM TermProgressEntity tp
        WHERE tp.user.id = :userId
          AND tp.glossary.id = :glossaryId
          AND tp.nextReviewAt IS NOT NULL
          AND tp.nextReviewAt <= :now
          AND tp.status IN :statuses
        ORDER BY tp.nextReviewAt ASC, tp.term.id ASC
        """
    )
    fun findDueProgresses(
        @Param("userId") userId: UUID,
        @Param("glossaryId") glossaryId: Long,
        @Param("now") now: Instant,
        @Param("statuses") statuses: Collection<TermProgressStatus>,
        pageable: Pageable
    ): List<TermProgressEntity>
}
