package com.snurdy.backend.domain.dictionary.glossary.repository

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface GlossaryTermRepository : JpaRepository<GlossaryTermEntity, GlossaryTermId> {
    fun deleteByIdTermId(termId: Long)
    fun deleteByIdGlossaryId(glossaryId: Long)
    fun deleteByIdGlossaryIdAndIdTermId(glossaryId: Long, termId: Long)
    fun deleteAllByIdGlossaryIdAndIdTermIdIn(glossaryId: Long, termIds: Collection<Long>)
    fun existsByIdGlossaryIdAndIdTermId(glossaryId: Long, termId: Long): Boolean
    fun countByIdGlossaryId(glossaryId: Long): Long
    fun findAllByIdGlossaryIdAndIdTermIdIn(glossaryId: Long, termIds: Collection<Long>): List<GlossaryTermEntity>

    @Query(
        value = """
        SELECT t.id AS termId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               t.explanation AS explanation
        FROM glossary_terms gt
        JOIN terms t ON t.id = gt.term_id
        WHERE gt.glossary_id = :glossaryId
          AND t.deleted_at IS NULL
        ORDER BY t.id
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM glossary_terms gt
        JOIN terms t ON t.id = gt.term_id
        WHERE gt.glossary_id = :glossaryId
          AND t.deleted_at IS NULL
        """,
        nativeQuery = true
    )
    fun findPagedSimpleTermsByGlossaryId(
        @Param("glossaryId") glossaryId: Long,
        pageable: Pageable
    ): Page<SimpleTermProjection>

    fun findAllByIdGlossaryIdInAndIdTermIdIn(
        glossaryIds: Collection<Long>,
        termIds: Collection<Long>
    ): List<GlossaryTermEntity>

    @Query(
        value = """
        SELECT t.id AS termId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               t.explanation AS explanation
        FROM glossary_terms gt
        JOIN terms t ON t.id = gt.term_id
        WHERE gt.glossary_id = :glossaryId
          AND t.deleted_at IS NULL
        """,
        nativeQuery = true
    )
    fun findAllSimpleTermsByGlossaryId(@Param("glossaryId") glossaryId: Long): List<SimpleTermProjection>

    // 학습 카드용 sampling. NEW (미학습 또는 status=NEW) 우선, 같은 그룹 내 random
    // requireAbbreviation = true 면 abbreviation 보유 단어만
    @Query(
        value = """
        SELECT t.id AS termId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               t.explanation AS explanation
        FROM glossary_terms gt
        JOIN terms t ON t.id = gt.term_id
        LEFT JOIN term_progresses tp
               ON tp.user_id = :userId
              AND tp.glossary_id = :glossaryId
              AND tp.term_id = t.id
        WHERE gt.glossary_id = :glossaryId
          AND t.deleted_at IS NULL
          AND (:requireAbbreviation = FALSE
               OR (t.abbreviation IS NOT NULL AND t.abbreviation <> ''))
        ORDER BY
          (tp.status IS NULL OR tp.status = 'NEW') DESC,
          RAND()
        LIMIT :count
        """,
        nativeQuery = true
    )
    fun sampleCardTerms(
        @Param("userId") userId: UUID,
        @Param("glossaryId") glossaryId: Long,
        @Param("requireAbbreviation") requireAbbreviation: Boolean,
        @Param("count") count: Int
    ): List<SimpleTermProjection>

    @Query(
        value = """
        SELECT glossary_id AS glossaryId, COUNT(*) AS mappedTermCount
        FROM glossary_terms
        GROUP BY glossary_id
        ORDER BY glossary_id
        """,
        nativeQuery = true
    )
    fun findGlossaryIdDistribution(): List<GlossaryIdDistributionProjection>

    @Query(
        value = """
        SELECT g.id AS glossaryId, g.category AS category, g.title AS title
        FROM glossary_terms gt
        JOIN glossaries g ON g.id = gt.glossary_id
        WHERE gt.term_id = :termId
        ORDER BY g.id
        """,
        nativeQuery = true
    )
    fun findGlossariesByTermId(@Param("termId") termId: Long): List<TermGlossaryProjection>

    /**
     * 글쓴이 용어 목록을 DB에서 filter + paging.
     * filter: "ALL" | "LEARNED" | "NOT_LEARNED"
     */
    @Query(
        value = """
        SELECT t.id AS termId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               COALESCE(tp.status, 'NEW') AS status,
               (CASE WHEN EXISTS (
                   SELECT 1 FROM bookmarks b
                   WHERE b.user_id = :userId
                     AND b.glossary_id = :glossaryId
                     AND b.term_id = t.id
               ) THEN 1 ELSE 0 END) AS bookmarked
        FROM glossary_terms gt
        JOIN terms t ON t.id = gt.term_id
        LEFT JOIN term_progresses tp
               ON tp.user_id = :userId
              AND tp.glossary_id = :glossaryId
              AND tp.term_id = t.id
        WHERE gt.glossary_id = :glossaryId
          AND t.deleted_at IS NULL
          AND (:filter = 'ALL'
               OR (:filter = 'LEARNED' AND tp.status IS NOT NULL AND tp.status <> 'NEW')
               OR (:filter = 'NOT_LEARNED' AND (tp.status IS NULL OR tp.status = 'NEW')))
        ORDER BY t.id
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM glossary_terms gt
        JOIN terms t ON t.id = gt.term_id
        LEFT JOIN term_progresses tp
               ON tp.user_id = :userId
              AND tp.glossary_id = :glossaryId
              AND tp.term_id = t.id
        WHERE gt.glossary_id = :glossaryId
          AND t.deleted_at IS NULL
          AND (:filter = 'ALL'
               OR (:filter = 'LEARNED' AND tp.status IS NOT NULL AND tp.status <> 'NEW')
               OR (:filter = 'NOT_LEARNED' AND (tp.status IS NULL OR tp.status = 'NEW')))
        """,
        nativeQuery = true
    )
    fun findPagedClientGlossaryTerms(
        @Param("userId") userId: UUID,
        @Param("glossaryId") glossaryId: Long,
        @Param("filter") filter: String,
        pageable: Pageable
    ): Page<ClientGlossaryTermProjection>

    @Query(
        value = """
        SELECT gt.term_id AS termId, g.id AS glossaryId, g.category AS category, g.title AS title
        FROM glossary_terms gt
        JOIN glossaries g ON g.id = gt.glossary_id
        JOIN terms t ON t.id = gt.term_id
        WHERE gt.term_id IN :termIds
          AND t.deleted_at IS NULL
        ORDER BY gt.term_id, g.id
        """,
        nativeQuery = true
    )
    fun findGlossariesByTermIds(@Param("termIds") termIds: List<Long>): List<BulkTermGlossaryProjection>

    // 테스트 가용 단어 수 집계 — totalCount + abbreviationCount 한 SELECT 로
    // dashboard selectedGlossary 의 availableCounts 매핑 입력
    @Query(
        value = """
        SELECT COUNT(*) AS totalCount,
               SUM(CASE WHEN t.abbreviation IS NOT NULL AND t.abbreviation <> '' THEN 1 ELSE 0 END) AS abbreviationCount
          FROM glossary_terms gt
          JOIN terms t ON t.id = gt.term_id
         WHERE gt.glossary_id = :glossaryId
           AND t.deleted_at IS NULL
        """,
        nativeQuery = true
    )
    fun countAvailableTerms(@Param("glossaryId") glossaryId: Long): GlossaryAvailableCountProjection

}

interface SimpleTermProjection {
    val termId: Long
    val spelling: String
    val meaning: String
    val abbreviation: String?
    val explanation: String?
}

interface BulkTermGlossaryProjection {
    val termId: Long
    val glossaryId: Long
    val category: String
    val title: String
}

interface ClientGlossaryTermProjection {
    val termId: Long
    val spelling: String
    val meaning: String
    val abbreviation: String?
    val status: String
    val bookmarked: Int
}

interface GlossaryIdDistributionProjection {
    val glossaryId: Long
    val mappedTermCount: Long
}

interface TermGlossaryProjection {
    val glossaryId: Long
    val category: String
    val title: String
}

interface GlossaryAvailableCountProjection {
    val totalCount: Long
    val abbreviationCount: Long
}
