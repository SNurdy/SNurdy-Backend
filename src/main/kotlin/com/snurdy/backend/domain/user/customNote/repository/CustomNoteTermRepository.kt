package com.snurdy.backend.domain.user.customNote.repository

import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteTermEntity
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteTermId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CustomNoteTermRepository : JpaRepository<CustomNoteTermEntity, CustomNoteTermId> {

    fun countByIdNoteId(noteId: Long): Long

    // 여러 노트의 단어 수를 한 번에 조회
    // 목록 화면에서 N+1 회피 용도
    @Query(
        """
        SELECT cnt.id.noteId AS noteId, COUNT(cnt) AS termCount
          FROM CustomNoteTermEntity cnt
         WHERE cnt.id.noteId IN :noteIds
         GROUP BY cnt.id.noteId
        """
    )
    fun countByNoteIds(@Param("noteIds") noteIds: Collection<Long>): List<NoteTermCountProjection>

    fun existsByIdNoteIdAndIdTermId(noteId: Long, termId: Long): Boolean

    fun findAllByIdNoteIdAndIdTermIdIn(noteId: Long, termIds: Collection<Long>): List<CustomNoteTermEntity>

    fun deleteByIdNoteIdAndIdTermId(noteId: Long, termId: Long)

    fun deleteAllByIdNoteIdAndIdTermIdIn(noteId: Long, termIds: Collection<Long>)

    fun deleteByIdNoteId(noteId: Long)

    // 단어장 안 단어 페이지 조회 (term 정보 join)
    @Query(
        value = """
        SELECT t.id AS termId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation
          FROM custom_note_terms cnt
          JOIN terms t ON t.id = cnt.term_id AND t.deleted_at IS NULL
         WHERE cnt.note_id = :noteId
         ORDER BY cnt.created_at DESC, t.id ASC
        """,
        countQuery = """
        SELECT COUNT(*)
          FROM custom_note_terms cnt
          JOIN terms t ON t.id = cnt.term_id AND t.deleted_at IS NULL
         WHERE cnt.note_id = :noteId
        """,
        nativeQuery = true
    )
    fun findPagedTermsByNoteId(@Param("noteId") noteId: Long, pageable: Pageable): Page<CustomNoteTermProjection>

    // 한 노트의 매핑을 다른 노트로 일괄 복사 (import 용)
    @Modifying
    @Query(
        value = """
        INSERT INTO custom_note_terms (note_id, term_id, created_at)
        SELECT :targetNoteId, term_id, CURRENT_TIMESTAMP
          FROM custom_note_terms
         WHERE note_id = :sourceNoteId
        """,
        nativeQuery = true
    )
    fun copyTermsFromNote(
        @Param("sourceNoteId") sourceNoteId: Long,
        @Param("targetNoteId") targetNoteId: Long
    ): Int

    // 학습 카드 sampling. createdAt 순 (Phase 2.10 결정 E1)
    // requireAbbreviation = true 면 abbreviation 보유 단어만
    @Query(
        value = """
        SELECT t.id AS termId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               t.explanation AS explanation
          FROM custom_note_terms cnt
          JOIN terms t ON t.id = cnt.term_id
         WHERE cnt.note_id = :noteId
           AND t.deleted_at IS NULL
           AND (:requireAbbreviation = FALSE
                OR (t.abbreviation IS NOT NULL AND t.abbreviation <> ''))
         ORDER BY cnt.created_at ASC, t.id ASC
         LIMIT :count
        """,
        nativeQuery = true
    )
    fun sampleCardTerms(
        @Param("noteId") noteId: Long,
        @Param("requireAbbreviation") requireAbbreviation: Boolean,
        @Param("count") count: Int
    ): List<SimpleTermProjection>

    // distractor 1차 풀 — 같은 노트의 전체 단어 (deletedAt 활성)
    @Query(
        value = """
        SELECT t.id AS termId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               t.explanation AS explanation
          FROM custom_note_terms cnt
          JOIN terms t ON t.id = cnt.term_id
         WHERE cnt.note_id = :noteId
           AND t.deleted_at IS NULL
        """,
        nativeQuery = true
    )
    fun findAllSimpleTermsByNoteId(@Param("noteId") noteId: Long): List<SimpleTermProjection>
}

interface CustomNoteTermProjection {
    val termId: Long
    val spelling: String
    val meaning: String
    val abbreviation: String?
}

interface NoteTermCountProjection {
    val noteId: Long
    val termCount: Long
}
