package com.snurdy.backend.domain.dictionary.term.repository

import com.snurdy.backend.domain.dictionary.term.entity.TermMorphemeEntity
import com.snurdy.backend.domain.dictionary.term.entity.TermMorphemeId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TermMorphemeRepository : JpaRepository<TermMorphemeEntity, TermMorphemeId> {
    fun deleteByIdTermId(termId: Long)

    @Query(
        value = """
        SELECT tm.term_id AS termId,
               MIN(tm.position) AS minPosition,
               MAX(tm.position) AS maxPosition,
               COUNT(*) AS rowCount,
               COUNT(DISTINCT tm.position) AS distinctPositionCount
        FROM term_morphemes tm
        GROUP BY tm.term_id
        HAVING MIN(tm.position) <> 0
            OR (MAX(tm.position) + 1) <> COUNT(*)
            OR COUNT(DISTINCT tm.position) <> COUNT(*)
        ORDER BY tm.term_id
        """,
        nativeQuery = true
    )
    fun findPositionAnomalies(): List<TermMorphemePositionAnomalyProjection>

    @Query(
        value = """
        SELECT t.id AS termId, t.spelling AS spelling
        FROM terms t
        LEFT JOIN term_morphemes tm ON tm.term_id = t.id
        WHERE tm.term_id IS NULL
        ORDER BY t.id
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findTermsWithoutMorpheme(@Param("limit") limit: Int): List<TermMorphemeGapProjection>

    @Query(
        value = """
        SELECT tm.term_id AS termId, COUNT(*) AS morphemeCount
        FROM term_morphemes tm
        GROUP BY tm.term_id
        ORDER BY tm.term_id
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findTermMorphemeCountSamples(@Param("limit") limit: Int): List<TermMorphemeCountProjection>

    @Query(
        value = """
        SELECT COUNT(*)
        FROM term_morphemes tm
        LEFT JOIN morphemes m ON m.id = tm.morpheme_id
        WHERE m.id IS NULL
        """,
        nativeQuery = true
    )
    fun countMissingMorphemeReferences(): Long

    @Query(
        value = """
        SELECT m.id AS morphemeId, m.spelling AS spelling, m.meaning AS meaning, tm.position AS position
        FROM term_morphemes tm
        JOIN morphemes m ON m.id = tm.morpheme_id
        WHERE tm.term_id = :termId
        ORDER BY tm.position
        """,
        nativeQuery = true
    )
    fun findOrderedMorphemesByTermId(@Param("termId") termId: Long): List<OrderedMorphemeProjection>

    @Query(
        value = """
    SELECT m.id AS morphemeId, m.spelling AS spelling, m.meaning AS meaning, 
           tm.position AS position, tm.term_id AS termId
    FROM term_morphemes tm
    JOIN morphemes m ON m.id = tm.morpheme_id
    WHERE tm.term_id IN :termIds
    ORDER BY tm.term_id, tm.position
    """,
        nativeQuery = true
    )
    fun findOrderedMorphemesByTermIds(@Param("termIds") termIds: List<Long>): List<BulkOrderedMorphemeProjection>

}

interface BulkOrderedMorphemeProjection {
    val morphemeId: Long
    val spelling: String
    val meaning: String
    val position: Int
    val termId: Long
}

interface TermMorphemePositionAnomalyProjection {
    val termId: Long
    val minPosition: Int
    val maxPosition: Int
    val rowCount: Long
    val distinctPositionCount: Long
}

interface TermMorphemeGapProjection {
    val termId: Long
    val spelling: String
}

interface TermMorphemeCountProjection {
    val termId: Long
    val morphemeCount: Long
}

interface OrderedMorphemeProjection {
    val morphemeId: Long
    val spelling: String
    val meaning: String
    val position: Int
}
