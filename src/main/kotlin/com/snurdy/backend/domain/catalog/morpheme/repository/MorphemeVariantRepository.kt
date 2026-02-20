package com.snurdy.backend.domain.catalog.morpheme.repository

import com.snurdy.backend.domain.catalog.morpheme.entity.MorphemeVariantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MorphemeVariantRepository : JpaRepository<MorphemeVariantEntity, Long> {

    @Query(
        value = """
        SELECT mv.id AS id,
               mv.morpheme_id AS morphemeId,
               mv.variant AS variant,
               mv.base AS base
        FROM morpheme_variants mv
        WHERE mv.morpheme_id IN :morphemeIds
        ORDER BY mv.morpheme_id, mv.id
        """,
        nativeQuery = true
    )
    fun findByMorphemeIds(@Param("morphemeIds") morphemeIds: List<Long>): List<MorphemeVariantProjection>

    @Query(
        value = """
    SELECT DISTINCT tm.term_id AS termId, tm.morpheme_id AS morphemeId
    FROM morpheme_variants mv
    JOIN term_morphemes tm ON tm.morpheme_id = mv.morpheme_id
    WHERE mv.variant LIKE CONCAT(:keyword, '%')
    ORDER BY tm.term_id
    """,
        nativeQuery = true
    )
    fun findTermIdsByVariantPrefix(@Param("keyword") keyword: String): List<MorphemeSearchHitProjection>

}

interface MorphemeSearchHitProjection {
    val termId: Long
    val morphemeId: Long
}

interface MorphemeVariantProjection {
    val id: Long
    val morphemeId: Long
    val variant: String
    val base: String
}