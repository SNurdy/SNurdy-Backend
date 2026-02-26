package com.snurdy.backend.domain.catalog.glossary.repository

import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GlossaryTermRepository : JpaRepository<GlossaryTermEntity, GlossaryTermId> {
    fun deleteByIdTermId(termId: Long)

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
        SELECT t.id AS termId, t.spelling AS spelling, t.meaning AS meaning
        FROM terms t
        LEFT JOIN glossary_terms gt ON gt.term_id = t.id
        WHERE gt.term_id IS NULL
          AND t.deleted_at IS NULL
        ORDER BY t.id
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findTermsWithoutGlossary(@Param("limit") limit: Int): List<TermSummaryProjection>

    @Query(
        value = """
        SELECT t.id AS termId, t.spelling AS spelling, t.meaning AS meaning
        FROM glossary_terms gt
        JOIN terms t ON t.id = gt.term_id
        WHERE gt.glossary_id = :glossaryId
          AND t.deleted_at IS NULL
        ORDER BY t.id
        """,
        nativeQuery = true
    )
    fun findTermSummariesByGlossaryId(@Param("glossaryId") glossaryId: Long): List<TermSummaryProjection>

    @Query(
        value = """
        SELECT t.id AS termId, t.spelling AS spelling, t.meaning AS meaning, t.abbreviation AS abbreviation
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
    fun findPagedTermSummariesByGlossaryId(
        @Param("glossaryId") glossaryId: Long,
        pageable: Pageable
    ): Page<GlossaryTermPageProjection>

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

    @Query(
        value = """
    SELECT gt.term_id AS termId, g.id AS glossaryId, g.category AS category, g.title AS title
    FROM glossary_terms gt
    JOIN glossaries g ON g.id = gt.glossary_id
    WHERE gt.term_id IN :termIds
    ORDER BY gt.term_id, g.id
    """,
        nativeQuery = true
    )
    fun findGlossariesByTermIds(@Param("termIds") termIds: List<Long>): List<BulkTermGlossaryProjection>

}

interface BulkTermGlossaryProjection {
    val termId: Long
    val glossaryId: Long
    val category: String
    val title: String
}

interface GlossaryIdDistributionProjection {
    val glossaryId: Long
    val mappedTermCount: Long
}

interface TermSummaryProjection {
    val termId: Long
    val spelling: String
    val meaning: String
}

interface GlossaryTermPageProjection {
    val termId: Long
    val spelling: String
    val meaning: String
    val abbreviation: String?
}

interface TermGlossaryProjection {
    val glossaryId: Long
    val category: String
    val title: String
}