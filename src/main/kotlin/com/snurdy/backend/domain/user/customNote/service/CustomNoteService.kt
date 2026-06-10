package com.snurdy.backend.domain.user.customNote.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.user.customNote.dto.AddTermItem
import com.snurdy.backend.domain.user.customNote.dto.AddTermToCustomNotesResponse
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
    private val termRepository: TermRepository,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository
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

        // batch count 로 N+1 회피
        val countByNoteId = customNoteTermRepository
            .countByNoteIds(notes.map { it.id })
            .associate { it.noteId to it.termCount }

        val responses = notes.map { note ->
            note.toResponse(termCount = countByNoteId[note.id] ?: 0L)
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
    fun addTerms(userId: UUID, noteId: Long, items: List<AddTermItem>): AddTermsResponse {
        val note = findOwned(userId, noteId)

        val currentTermCount = customNoteTermRepository.countByIdNoteId(noteId)
        if (currentTermCount + items.size > MAX_TERMS_PER_NOTE) {
            throw ApiException(CustomNoteError.TERM_LIMIT_EXCEEDED)
        }

        val distinctItems = items.distinctBy { it.glossaryId to it.termId }

        val termIds = distinctItems.map { it.termId }.distinct()
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id }
        if (terms.size != termIds.size) {
            throw ApiException(DictionaryError.TERM_NOT_FOUND)
        }

        val glossaryIds = distinctItems.map { it.glossaryId }.distinct()
        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id }
        if (glossaries.size != glossaryIds.size) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        // (glossaryId, termId) 매핑이 실제 사전에 있는지 확인 — 임의의 쌍 입력 차단
        val validPairs = glossaryTermRepository
            .findAllByIdGlossaryIdInAndIdTermIdIn(glossaryIds, termIds)
            .mapTo(mutableSetOf()) { it.id.glossaryId to it.id.termId }
        val invalidItems = distinctItems.filter { (it.glossaryId to it.termId) !in validPairs }
        if (invalidItems.isNotEmpty()) {
            throw ApiException(DictionaryError.TERM_NOT_FOUND)
        }

        val existingPairs = customNoteTermRepository
            .findAllByIdNoteIdAndIdGlossaryIdInAndIdTermIdIn(noteId, glossaryIds, termIds)
            .mapTo(mutableSetOf()) { it.id.glossaryId to it.id.termId }

        val newItems = distinctItems.filter { (it.glossaryId to it.termId) !in existingPairs }
        if (newItems.isEmpty()) {
            return AddTermsResponse(
                added = 0,
                skipped = distinctItems.size,
                totalTermCount = currentTermCount
            )
        }

        val newEntities = newItems.map { item ->
            CustomNoteTermEntity(
                id = CustomNoteTermId(noteId = noteId, glossaryId = item.glossaryId, termId = item.termId),
                note = note,
                glossary = glossaries[item.glossaryId]!!,
                term = terms[item.termId]!!
            )
        }
        customNoteTermRepository.saveAll(newEntities)

        return AddTermsResponse(
            added = newItems.size,
            skipped = distinctItems.size - newItems.size,
            totalTermCount = currentTermCount + newItems.size
        )
    }

    // 사전 단어 1개를 N개 커스텀 단어장에 동시 추가
    // 이미 단어가 있는 노트는 skip, 새로 추가될 노트가 termLimit 초과면 전체 거부
    @Transactional
    fun addTermToNotes(
        userId: UUID,
        glossaryId: Long,
        termId: Long,
        noteIds: List<Long>
    ): AddTermToCustomNotesResponse {
        val distinctNoteIds = noteIds.distinct()

        val notes = customNoteRepository.findAllById(distinctNoteIds)
        if (notes.size != distinctNoteIds.size) {
            throw ApiException(CustomNoteError.NOT_FOUND)
        }
        notes.forEach { note ->
            if (note.ownerId != userId) throw ApiException(CustomNoteError.FORBIDDEN)
        }

        val term = termRepository.findByIdInAndDeletedAtIsNull(listOf(termId)).firstOrNull()
            ?: throw ApiException(DictionaryError.TERM_NOT_FOUND)
        val glossary = glossaryRepository.findById(glossaryId).orElseThrow {
            ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
        val validPair = glossaryTermRepository
            .findAllByIdGlossaryIdInAndIdTermIdIn(listOf(glossaryId), listOf(termId))
            .any { it.id.glossaryId == glossaryId && it.id.termId == termId }
        if (!validPair) {
            throw ApiException(DictionaryError.TERM_NOT_FOUND)
        }

        // PK 직접 조회로 어느 노트에 이미 있는지 가려냄
        val existingNoteIds = customNoteTermRepository
            .findAllById(distinctNoteIds.map { CustomNoteTermId(noteId = it, glossaryId = glossaryId, termId = termId) })
            .mapTo(mutableSetOf()) { it.id.noteId }

        val targetNoteIds = distinctNoteIds.filter { it !in existingNoteIds }
        if (targetNoteIds.isEmpty()) {
            return AddTermToCustomNotesResponse(
                addedNoteIds = emptyList(),
                skippedNoteIds = distinctNoteIds
            )
        }

        // 추가 대상 노트의 현재 termCount 확인 (단어장당 최대 250)
        val termCountByNoteId = customNoteTermRepository
            .countByNoteIds(targetNoteIds)
            .associate { it.noteId to it.termCount }
        val notesById = notes.associateBy { it.id }
        targetNoteIds.forEach { noteId ->
            val current = termCountByNoteId[noteId] ?: 0L
            if (current + 1 > MAX_TERMS_PER_NOTE) {
                throw ApiException(CustomNoteError.TERM_LIMIT_EXCEEDED)
            }
        }

        val newEntities = targetNoteIds.map { noteId ->
            CustomNoteTermEntity(
                id = CustomNoteTermId(noteId = noteId, glossaryId = glossaryId, termId = termId),
                note = notesById[noteId]!!,
                glossary = glossary,
                term = term
            )
        }
        customNoteTermRepository.saveAll(newEntities)

        return AddTermToCustomNotesResponse(
            addedNoteIds = targetNoteIds,
            skippedNoteIds = distinctNoteIds.filter { it in existingNoteIds }
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
    fun deleteTerms(userId: UUID, noteId: Long, items: List<AddTermItem>) {
        findOwned(userId, noteId)
        val distinctItems = items.distinctBy { it.glossaryId to it.termId }

        val glossaryIds = distinctItems.map { it.glossaryId }.distinct()
        val termIds = distinctItems.map { it.termId }.distinct()

        val existingPairs = customNoteTermRepository
            .findAllByIdNoteIdAndIdGlossaryIdInAndIdTermIdIn(noteId, glossaryIds, termIds)
            .mapTo(mutableSetOf()) { it.id.glossaryId to it.id.termId }

        // 하나라도 없으면 전체 거부 (단일 삭제와 일관)
        val requested = distinctItems.mapTo(mutableSetOf()) { it.glossaryId to it.termId }
        if (existingPairs != requested) {
            throw ApiException(CustomNoteError.TERM_NOT_IN_NOTE)
        }

        distinctItems.forEach { item ->
            customNoteTermRepository.deleteByIdNoteIdAndIdGlossaryIdAndIdTermId(noteId, item.glossaryId, item.termId)
        }
    }

    @Transactional
    fun deleteTerm(userId: UUID, noteId: Long, glossaryId: Long, termId: Long) {
        findOwned(userId, noteId)
        if (!customNoteTermRepository.existsByIdNoteIdAndIdGlossaryIdAndIdTermId(noteId, glossaryId, termId)) {
            throw ApiException(CustomNoteError.TERM_NOT_IN_NOTE)
        }
        customNoteTermRepository.deleteByIdNoteIdAndIdGlossaryIdAndIdTermId(noteId, glossaryId, termId)
    }

    private fun findOwned(userId: UUID, noteId: Long): CustomNoteEntity {
        val note = customNoteRepository.findByIdOrNull(noteId)
            ?: throw ApiException(CustomNoteError.NOT_FOUND)
        if (note.ownerId != userId) {
            throw ApiException(CustomNoteError.FORBIDDEN)
        }
        return note
    }

    // 다른 도메인 (test 등) 에서 커스텀노트 소유 검증만 필요할 때 호출
    // 존재 안 함 -> NOT_FOUND, 소유 불일치 -> FORBIDDEN
    @Transactional(readOnly = true)
    fun requireNoteOwnership(userId: UUID, noteId: Long) {
        findOwned(userId, noteId)
    }

    companion object {
        private const val MAX_NOTES_PER_USER = 20L
        private const val MAX_TERMS_PER_NOTE = 250L
    }
}
