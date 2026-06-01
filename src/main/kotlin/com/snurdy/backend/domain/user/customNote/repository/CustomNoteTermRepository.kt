package com.snurdy.backend.domain.user.customNote.repository

import com.snurdy.backend.domain.user.customNote.entity.CustomNoteTermEntity
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteTermId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CustomNoteTermRepository : JpaRepository<CustomNoteTermEntity, CustomNoteTermId> {

    fun countByIdNoteId(noteId: Long): Long

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
}

interface CustomNoteTermProjection {
    val termId: Long
    val spelling: String
    val meaning: String
    val abbreviation: String?
}
