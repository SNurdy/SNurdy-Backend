package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface TermProgressRepository : JpaRepository<TermProgressEntity, UUID> {
    fun existsByUserId(userId: UUID): Boolean

    fun countByUserId(userId: UUID): Long

    fun deleteByUserIdAndGlossaryId(userId: UUID, glossaryId: Long)

    fun deleteByUserIdAndGlossaryIdAndTermId(userId: UUID, glossaryId: Long, termId: Long)

    fun findByUserIdAndGlossaryIdAndTermId(userId: UUID, glossaryId: Long, termId: Long): TermProgressEntity?

    fun findByUserIdAndGlossaryIdAndTermIdIn(
        userId: UUID,
        glossaryId: Long,
        termIds: List<Long>
    ): List<TermProgressEntity>

    // N+1 회피 위해 term, glossary 를 fetch join
    // 호출 측에서 it.term.id 와 it.glossary.id 를 모두 접근하기 때문
    @EntityGraph(attributePaths = ["term", "glossary"])
    fun findAllByUserIdAndGlossaryIdInAndTermIdIn(
        userId: UUID,
        glossaryIds: Collection<Long>,
        termIds: Collection<Long>
    ): List<TermProgressEntity>

    // N+1 회피 위해 term 을 fetch join
    // 호출 측에서 progress.term.id 를 반복 접근하기 때문
    @EntityGraph(attributePaths = ["term"])
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

    // 대시보드 과 지표 리스트의 과별 학습 단어 수 batch
    // after 가 null 이면 전체 누적, 아니면 lastReviewedAt 가 after 이후인 distinct 단어 수
    // glossary 별 그루핑 카운트로 N+1 회피
    @Query(
        """
        SELECT tp.glossary.id AS glossaryId, COUNT(tp) AS termCount
        FROM TermProgressEntity tp
        WHERE tp.user.id = :userId
          AND tp.glossary.id IN :glossaryIds
          AND tp.lastReviewedAt IS NOT NULL
          AND (:after IS NULL OR tp.lastReviewedAt >= :after)
        GROUP BY tp.glossary.id
        """
    )
    fun countLearnedSinceByGlossaries(
        @Param("userId") userId: UUID,
        @Param("glossaryIds") glossaryIds: Collection<Long>,
        @Param("after") after: Instant?
    ): List<GlossaryLearnedCountProjection>
}

interface GlossaryLearnedCountProjection {
    val glossaryId: Long
    val termCount: Long
}
