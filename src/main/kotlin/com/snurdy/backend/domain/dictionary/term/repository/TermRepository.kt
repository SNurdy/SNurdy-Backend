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

    fun findByDeletedAtIsNull(pageable: Pageable): Page<TermEntity>

    @Query(value = "SELECT spelling FROM terms WHERE deleted_at IS NULL ORDER BY id", nativeQuery = true)
    fun findAllSpellings(): List<String>

    // ft_terms_search (spelling, meaning, abbreviation) WITH PARSER ngram 인덱스 사용.
    // BOOLEAN MODE: 50% 초과 등장 단어를 무시하는 NATURAL LANGUAGE MODE 제약 없음.
    // 호출 전에 서비스에서 query가 non-blank임을 보장해야 한다.
    @Query(
        value = """
        SELECT *
        FROM terms
        WHERE deleted_at IS NULL
          AND MATCH(spelling, meaning, abbreviation) AGAINST(:query IN BOOLEAN MODE)
        ORDER BY MATCH(spelling, meaning, abbreviation) AGAINST(:query IN BOOLEAN MODE) DESC, id ASC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM terms
        WHERE deleted_at IS NULL
          AND MATCH(spelling, meaning, abbreviation) AGAINST(:query IN BOOLEAN MODE)
        """,
        nativeQuery = true
    )
    fun searchActiveTerms(@Param("query") query: String, pageable: Pageable): Page<TermEntity>

    fun findBySpellingContainingIgnoreCaseOrMeaningContainingIgnoreCaseOrAbbreviationContainingIgnoreCase(
        spelling: String,
        meaning: String,
        abbreviation: String,
        pageable: Pageable
    ): Page<TermEntity>

    @Query(
        value = """
        SELECT t FROM TermEntity t
        WHERE t.deletedAt IS NULL
          AND (LOWER(t.spelling) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.meaning) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.abbreviation) LIKE LOWER(CONCAT('%', :q, '%')))
        """,
        countQuery = """
        SELECT COUNT(t) FROM TermEntity t
        WHERE t.deletedAt IS NULL
          AND (LOWER(t.spelling) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.meaning) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.abbreviation) LIKE LOWER(CONCAT('%', :q, '%')))
        """
    )
    fun findActiveByKeyword(@Param("q") q: String, pageable: Pageable): Page<TermEntity>

    fun findAllBySpellingIgnoreCase(spelling: String): List<TermEntity>

    fun findByIdAndDeletedAtIsNull(id: Long): Optional<TermEntity>
}
