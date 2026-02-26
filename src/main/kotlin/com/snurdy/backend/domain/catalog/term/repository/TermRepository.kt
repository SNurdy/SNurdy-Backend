package com.snurdy.backend.domain.catalog.term.repository

import com.snurdy.backend.domain.catalog.term.entity.TermEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface TermRepository : JpaRepository<TermEntity, Long> {

    @Query(value = "SELECT spelling FROM terms WHERE deleted_at IS NULL ORDER BY id", nativeQuery = true)
    fun findAllSpellings(): List<String>

    fun findBySpellingContainingIgnoreCaseOrMeaningContainingIgnoreCaseOrAbbreviationContainingIgnoreCase(
        spelling: String,
        meaning: String,
        abbreviation: String,
        pageable: Pageable
    ): Page<TermEntity>

    fun findAllBySpellingIgnoreCase(spelling: String): List<TermEntity>

    fun findByIdAndDeletedAtIsNull(id: Long): Optional<TermEntity>
}