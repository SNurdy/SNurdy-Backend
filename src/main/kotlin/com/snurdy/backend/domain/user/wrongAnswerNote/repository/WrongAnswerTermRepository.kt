package com.snurdy.backend.domain.user.wrongAnswerNote.repository

import com.snurdy.backend.domain.user.wrongAnswerNote.entity.WrongAnswerTermEntity
import com.snurdy.backend.domain.user.wrongAnswerNote.entity.WrongAnswerTermId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface WrongAnswerTermRepository : JpaRepository<WrongAnswerTermEntity, WrongAnswerTermId> {

    fun findAllByIdUserIdAndIdTermIdInAndDeletedAtIsNull(
        userId: UUID,
        termIds: Collection<Long>
    ): List<WrongAnswerTermEntity>

    // 기간별 카드 카운트. after 가 null 이면 ALL
    @Query(
        """
        SELECT COUNT(w) FROM WrongAnswerTermEntity w
         WHERE w.id.userId = :userId
           AND w.deletedAt IS NULL
           AND (:after IS NULL OR w.incorrectAt >= :after)
        """
    )
    fun countActive(
        @Param("userId") userId: UUID,
        @Param("after") after: Instant?
    ): Long

    // 과별 그루핑 카운트 (source_glossary_id 별 단어 수)
    @Query(
        """
        SELECT w.sourceGlossaryId AS sourceGlossaryId, COUNT(w) AS incorrectTermCount
          FROM WrongAnswerTermEntity w
         WHERE w.id.userId = :userId
           AND w.deletedAt IS NULL
           AND w.sourceGlossaryId IS NOT NULL
         GROUP BY w.sourceGlossaryId
        """
    )
    fun groupBySourceGlossary(@Param("userId") userId: UUID): List<GlossaryWrongAnswerGroup>

    // 활성 단어 목록 — 검색어 없음 (incorrect_at DESC)
    @Query(
        value = """
        SELECT t.id AS id,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               wat.total_incorrect_count AS totalIncorrectCount
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:sourceGlossaryId IS NULL OR wat.source_glossary_id = :sourceGlossaryId)
         ORDER BY wat.incorrect_at DESC, t.id ASC
        """,
        countQuery = """
        SELECT COUNT(*)
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:sourceGlossaryId IS NULL OR wat.source_glossary_id = :sourceGlossaryId)
        """,
        nativeQuery = true
    )
    fun findActiveTerms(
        @Param("userId") userId: UUID,
        @Param("after") after: Instant?,
        @Param("sourceGlossaryId") sourceGlossaryId: Long?,
        pageable: Pageable
    ): Page<WrongAnswerTermItemProjection>

    // 활성 단어 목록 — 검색어 있음 (FULLTEXT MATCH AGAINST, relevance DESC)
    @Query(
        value = """
        SELECT t.id AS id,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               wat.total_incorrect_count AS totalIncorrectCount
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:sourceGlossaryId IS NULL OR wat.source_glossary_id = :sourceGlossaryId)
           AND MATCH(t.spelling, t.meaning, t.abbreviation) AGAINST(:query IN BOOLEAN MODE)
         ORDER BY MATCH(t.spelling, t.meaning, t.abbreviation) AGAINST(:query IN BOOLEAN MODE) DESC,
                  wat.incorrect_at DESC,
                  t.id ASC
        """,
        countQuery = """
        SELECT COUNT(*)
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:sourceGlossaryId IS NULL OR wat.source_glossary_id = :sourceGlossaryId)
           AND MATCH(t.spelling, t.meaning, t.abbreviation) AGAINST(:query IN BOOLEAN MODE)
        """,
        nativeQuery = true
    )
    fun searchActiveTerms(
        @Param("userId") userId: UUID,
        @Param("after") after: Instant?,
        @Param("sourceGlossaryId") sourceGlossaryId: Long?,
        @Param("query") query: String,
        pageable: Pageable
    ): Page<WrongAnswerTermItemProjection>

    // soft delete (다중)
    @Modifying
    @Query(
        """
        UPDATE WrongAnswerTermEntity w
           SET w.deletedAt = :now
         WHERE w.id.userId = :userId
           AND w.id.termId IN :termIds
           AND w.deletedAt IS NULL
        """
    )
    fun softDeleteByUserIdAndTermIds(
        @Param("userId") userId: UUID,
        @Param("termIds") termIds: Collection<Long>,
        @Param("now") now: Instant
    ): Int
}

interface WrongAnswerTermItemProjection {
    val id: Long
    val spelling: String
    val meaning: String
    val abbreviation: String?
    val totalIncorrectCount: Int
}

interface GlossaryWrongAnswerGroup {
    val sourceGlossaryId: Long
    val incorrectTermCount: Long
}
