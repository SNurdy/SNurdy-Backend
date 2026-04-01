package com.snurdy.backend.domain.dictionary.term.repository

import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface TermRepository : JpaRepository<TermEntity, Long> {

    fun findByIdInAndDeletedAtIsNull(ids: List<Long>): List<TermEntity>

    @Query(value = "SELECT spelling FROM terms WHERE deleted_at IS NULL ORDER BY id", nativeQuery = true)
    fun findAllSpellings(): List<String>

    @Query(
        """
        SELECT t
        FROM TermEntity t
        WHERE t.deletedAt IS NULL
          AND (
            :query IS NULL
            OR TRIM(:query) = ''
            OR LOWER(t.spelling) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(t.meaning) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(t.abbreviation, '')) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """
    )
    fun searchActiveTerms(@Param("query") query: String?, pageable: Pageable): Page<TermEntity>

    fun findBySpellingContainingIgnoreCaseOrMeaningContainingIgnoreCaseOrAbbreviationContainingIgnoreCase(
        spelling: String,
        meaning: String,
        abbreviation: String,
        pageable: Pageable
    ): Page<TermEntity>

    fun findAllBySpellingIgnoreCase(spelling: String): List<TermEntity>

    fun findByIdAndDeletedAtIsNull(id: Long): Optional<TermEntity>
}
