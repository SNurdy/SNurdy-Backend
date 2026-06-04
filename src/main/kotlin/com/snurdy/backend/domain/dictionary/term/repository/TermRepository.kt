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

    // 사전 화면용 통합 검색: keyword 검색 + category/glossaryId 필터를 모두 지원.
    // 각 파라미터가 NULL 이면 해당 조건은 무시된다.
    // 정렬: 항상 spelling 알파벳 오름차순 (사전 화면 인덱스 일관성을 위해)
    // glossary 필터 적용 시 JOIN 에 의한 중복 행은 DISTINCT 로 제거한다.
    @Query(
        value = """
        SELECT DISTINCT t.*
        FROM terms t
        LEFT JOIN glossary_terms gt ON gt.term_id = t.id
        LEFT JOIN glossaries g ON g.id = gt.glossary_id
        LEFT JOIN term_morphemes tm ON tm.term_id = t.id
        WHERE t.deleted_at IS NULL
          AND (:query IS NULL OR MATCH(t.spelling, t.meaning, t.abbreviation) AGAINST(:query IN BOOLEAN MODE))
          AND (:category IS NULL OR g.category = :category)
          AND (:glossaryId IS NULL OR g.id = :glossaryId)
          AND (:morphemeId IS NULL OR tm.morpheme_id = :morphemeId)
        ORDER BY t.spelling
        """,
        countQuery = """
        SELECT COUNT(DISTINCT t.id)
        FROM terms t
        LEFT JOIN glossary_terms gt ON gt.term_id = t.id
        LEFT JOIN glossaries g ON g.id = gt.glossary_id
        LEFT JOIN term_morphemes tm ON tm.term_id = t.id
        WHERE t.deleted_at IS NULL
          AND (:query IS NULL OR MATCH(t.spelling, t.meaning, t.abbreviation) AGAINST(:query IN BOOLEAN MODE))
          AND (:category IS NULL OR g.category = :category)
          AND (:glossaryId IS NULL OR g.id = :glossaryId)
          AND (:morphemeId IS NULL OR tm.morpheme_id = :morphemeId)
        """,
        nativeQuery = true
    )
    fun findFilteredTerms(
        @Param("query") query: String?,
        @Param("category") category: String?,
        @Param("glossaryId") glossaryId: Long?,
        @Param("morphemeId") morphemeId: Long?,
        pageable: Pageable
    ): Page<TermEntity>
}
