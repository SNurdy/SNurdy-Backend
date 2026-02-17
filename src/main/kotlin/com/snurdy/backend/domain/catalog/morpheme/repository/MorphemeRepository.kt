package com.snurdy.backend.domain.catalog.morpheme.repository

import com.snurdy.backend.domain.catalog.morpheme.entity.MorphemeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MorphemeRepository : JpaRepository<MorphemeEntity, Long> {

    @Query(
        value = """
        SELECT COUNT(*)
        FROM morphemes m
        LEFT JOIN term_morphemes tm ON tm.morpheme_id = m.id
        WHERE tm.morpheme_id IS NULL
        """,
        nativeQuery = true
    )
    fun countOrphanMorphemes(): Long

    @Query(
        value = """
        SELECT m.id AS morphemeId, m.spelling AS spelling, m.meaning AS meaning
        FROM morphemes m
        LEFT JOIN term_morphemes tm ON tm.morpheme_id = m.id
        WHERE tm.morpheme_id IS NULL
        ORDER BY m.id
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findOrphanMorphemeSamples(@Param("limit") limit: Int): List<OrphanMorphemeProjection>
}

interface OrphanMorphemeProjection {
    val morphemeId: Long
    val spelling: String
    val meaning: String
}
