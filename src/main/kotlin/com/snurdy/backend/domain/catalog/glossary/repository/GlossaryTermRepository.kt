package com.snurdy.backend.domain.catalog.glossary.repository

import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GlossaryTermRepository : JpaRepository<GlossaryTermEntity, GlossaryTermId> {

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
        ORDER BY t.id
        """,
        nativeQuery = true
    )
    fun findTermSummariesByGlossaryId(@Param("glossaryId") glossaryId: Long): List<TermSummaryProjection>
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
