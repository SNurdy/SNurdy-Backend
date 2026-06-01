package com.snurdy.backend.domain.user.customNote.service

import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.user.customNote.dto.AddTermsResponse
import com.snurdy.backend.domain.user.customNote.dto.CreateCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteListResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteTermsResponse
import com.snurdy.backend.domain.user.customNote.dto.UpdateCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.toResponse
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteEntity
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteTermEntity
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteTermId
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteRepository
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteTermRepository
import com.snurdy.backend.global.constants.CustomNoteError
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CustomNoteService(
    private val customNoteRepository: CustomNoteRepository,
    private val customNoteTermRepository: CustomNoteTermRepository,
    private val termRepository: TermRepository
) {

    @Transactional
    fun create(userId: UUID, request: CreateCustomNoteRequest): CustomNoteResponse {
        if (customNoteRepository.countByOwnerId(userId) >= MAX_NOTES_PER_USER) {
            throw ApiException(CustomNoteError.NOTE_LIMIT_EXCEEDED)
        }

        val title = request.title.trim()
        if (customNoteRepository.existsByOwnerIdAndTitle(userId, title)) {
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        val saved = try {
            customNoteRepository.save(CustomNoteEntity(ownerId = userId, title = title))
        } catch (_: DataIntegrityViolationException) {
            // (owner_id, title) UNIQUE 제약 충돌 - 동시 생성 안전망
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        return saved.toResponse(termCount = 0L)
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID): CustomNoteListResponse {
        val notes = customNoteRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId)
        if (notes.isEmpty()) return CustomNoteListResponse(notes = emptyList())

        val responses = notes.map { note ->
            note.toResponse(termCount = customNoteTermRepository.countByIdNoteId(note.id))
        }
        return CustomNoteListResponse(notes = responses)
    }

    @Transactional(readOnly = true)
    fun get(userId: UUID, noteId: Long): CustomNoteResponse {
        val note = findOwned(userId, noteId)
        val termCount = customNoteTermRepository.countByIdNoteId(noteId)
        return note.toResponse(termCount = termCount)
    }

    @Transactional
    fun update(userId: UUID, noteId: Long, request: UpdateCustomNoteRequest): CustomNoteResponse {
        val note = findOwned(userId, noteId)

        val newTitle = request.title.trim()
        if (newTitle != note.title && customNoteRepository.existsByOwnerIdAndTitle(userId, newTitle)) {
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        note.rename(newTitle)
        try {
            customNoteRepository.save(note)
        } catch (_: DataIntegrityViolationException) {
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        val termCount = customNoteTermRepository.countByIdNoteId(noteId)
        return note.toResponse(termCount = termCount)
    }

    @Transactional
    fun deleteNote(userId: UUID, noteId: Long) {
        findOwned(userId, noteId)
        // custom_note_terms, custom_note_share_codes 는 ON DELETE CASCADE 로 자동 정리
        customNoteRepository.deleteById(noteId)
    }

    @Transactional
    fun deleteNotes(userId: UUID, noteIds: List<Long>) {
        val distinctNoteIds = noteIds.distinct()
        val notes = customNoteRepository.findAllById(distinctNoteIds)

        if (notes.size != distinctNoteIds.size) {
            throw ApiException(CustomNoteError.NOT_FOUND)
        }
        notes.forEach { note ->
            if (note.ownerId != userId) throw ApiException(CustomNoteError.FORBIDDEN)
        }

        customNoteRepository.deleteAllByIdInBatch(distinctNoteIds)
    }

    @Transactional
    fun addTerms(userId: UUID, noteId: Long, termIds: List<Long>): AddTermsResponse {
        val note = findOwned(userId, noteId)

        val currentTermCount = customNoteTermRepository.countByIdNoteId(noteId)
        if (currentTermCount + termIds.size > MAX_TERMS_PER_NOTE) {
            throw ApiException(CustomNoteError.TERM_LIMIT_EXCEEDED)
        }

        val distinctTermIds = termIds.distinct()
        val terms = termRepository.findByIdInAndDeletedAtIsNull(distinctTermIds).associateBy { it.id }
        if (terms.size != distinctTermIds.size) {
            throw ApiException(DictionaryError.TERM_NOT_FOUND)
        }

        val existingTermIds = customNoteTermRepository
            .findAllByIdNoteIdAndIdTermIdIn(noteId, distinctTermIds)
            .mapTo(mutableSetOf()) { it.id.termId }

        val newTermIds = distinctTermIds.filter { it !in existingTermIds }
        if (newTermIds.isEmpty()) {
            return AddTermsResponse(
                added = 0,
                skipped = distinctTermIds.size,
                totalTermCount = currentTermCount
            )
        }

        val newEntities = newTermIds.map { termId ->
            CustomNoteTermEntity(
                id = CustomNoteTermId(noteId = noteId, termId = termId),
                note = note,
                term = terms[termId]!!
            )
        }
        customNoteTermRepository.saveAll(newEntities)

        return AddTermsResponse(
            added = newTermIds.size,
            skipped = distinctTermIds.size - newTermIds.size,
            totalTermCount = currentTermCount + newTermIds.size
        )
    }

    @Transactional(readOnly = true)
    fun listTerms(userId: UUID, noteId: Long, page: Int, size: Int): CustomNoteTermsResponse {
        val note = findOwned(userId, noteId)
        val pageable = PageRequest.of(page, size)
        val result = customNoteTermRepository.findPagedTermsByNoteId(noteId, pageable)
        return CustomNoteTermsResponse(
            noteId = note.id,
            title = note.title,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
            terms = result.content.map { it.toResponse() }
        )
    }

    @Transactional
    fun deleteTerms(userId: UUID, noteId: Long, termIds: List<Long>) {
        findOwned(userId, noteId)
        val distinctTermIds = termIds.distinct()

        val existing = customNoteTermRepository
            .findAllByIdNoteIdAndIdTermIdIn(noteId, distinctTermIds)
            .mapTo(mutableSetOf()) { it.id.termId }

        // 하나라도 없으면 전체 거부 (단일 삭제와 일관)
        if (existing.size != distinctTermIds.size) {
            throw ApiException(CustomNoteError.TERM_NOT_IN_NOTE)
        }

        customNoteTermRepository.deleteAllByIdNoteIdAndIdTermIdIn(noteId, distinctTermIds)
    }

    @Transactional
    fun deleteTerm(userId: UUID, noteId: Long, termId: Long) {
        findOwned(userId, noteId)
        if (!customNoteTermRepository.existsByIdNoteIdAndIdTermId(noteId, termId)) {
            throw ApiException(CustomNoteError.TERM_NOT_IN_NOTE)
        }
        customNoteTermRepository.deleteByIdNoteIdAndIdTermId(noteId, termId)
    }

    private fun findOwned(userId: UUID, noteId: Long): CustomNoteEntity {
        val note = customNoteRepository.findByIdOrNull(noteId)
            ?: throw ApiException(CustomNoteError.NOT_FOUND)
        if (note.ownerId != userId) {
            throw ApiException(CustomNoteError.FORBIDDEN)
        }
        return note
    }

    companion object {
        private const val MAX_NOTES_PER_USER = 20L
        private const val MAX_TERMS_PER_NOTE = 250L
    }
}
