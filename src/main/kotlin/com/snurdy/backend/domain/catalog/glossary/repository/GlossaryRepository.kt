package com.snurdy.backend.domain.catalog.glossary.repository

import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GlossaryRepository : JpaRepository<GlossaryEntity, Long> {

    fun findByTitle(title: String): GlossaryEntity?
    fun findByCategoryContainingIgnoreCaseOrTitleContainingIgnoreCase(category: String, title: String, pageable: Pageable): Page<GlossaryEntity>

    @Query("SELECT DISTINCT g.category FROM GlossaryEntity g ORDER BY g.category")
    fun findDistinctCategories(): List<String>

    @Query(
        value = """
        SELECT g.id AS glossaryId, g.category AS category, g.title AS title, COUNT(gt.term_id) AS termCount
        FROM glossaries g
        LEFT JOIN glossary_terms gt ON gt.glossary_id = g.id
        GROUP BY g.id, g.category, g.title
        ORDER BY g.id
        """,
        nativeQuery = true
    )
    fun findGlossaryTermCounts(): List<GlossaryTermCountProjection>
}

interface GlossaryTermCountProjection {
    val glossaryId: Long
    val category: String
    val title: String
    val termCount: Long
}
