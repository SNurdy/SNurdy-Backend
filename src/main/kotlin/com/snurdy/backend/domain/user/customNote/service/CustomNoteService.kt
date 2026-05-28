package com.snurdy.backend.domain.user.customNote.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryType
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.user.customNote.dto.AddTermsResponse
import com.snurdy.backend.domain.user.customNote.dto.CreateCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteListResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteTermsResponse
import com.snurdy.backend.domain.user.customNote.dto.UpdateCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.toResponse
import org.springframework.data.domain.PageRequest
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.CustomNoteError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CustomNoteService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val bookmarkRepository: BookmarkRepository
) {

    @Transactional
    fun create(userId: UUID, request: CreateCustomNoteRequest): CustomNoteResponse {
        if (glossaryRepository.countByOwnerIdAndType(userId, GlossaryType.CUSTOM) >= MAX_NOTES_PER_USER) {
            throw ApiException(CustomNoteError.NOTE_LIMIT_EXCEEDED)
        }

        val resolvedTitle = resolveTitle(userId, request.title)

        // 사전 중복 체크 (자동 생성 경로에서는 충돌하지 않지만 안전망)
        if (glossaryRepository.existsByOwnerIdAndTitle(userId, resolvedTitle)) {
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        val saved = try {
            glossaryRepository.save(
                GlossaryEntity(
                    category = CUSTOM_CATEGORY,
                    title = resolvedTitle,
                    type = GlossaryType.CUSTOM,
                    ownerId = userId
                )
            )
        } catch (_: DataIntegrityViolationException) {
            // (owner_id, title) UNIQUE 제약 충돌
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        return saved.toResponse(learnedCount = 0, termCount = 0L)
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID): CustomNoteListResponse {
        val notes = glossaryRepository.findAllByOwnerIdAndTypeOrderByCreatedAtDesc(userId, GlossaryType.CUSTOM)
        if (notes.isEmpty()) return CustomNoteListResponse(notes = emptyList())

        val progressByGlossaryId = glossaryProgressRepository.findAllByIdUserId(userId)
            .associateBy { it.id.glossaryId }

        val responses = notes.map { glossary ->
            val termCount = glossaryTermRepository.countByIdGlossaryId(glossary.id)
            val learnedCount = progressByGlossaryId[glossary.id]?.learnedCount ?: 0
            glossary.toResponse(learnedCount = learnedCount, termCount = termCount)
        }

        return CustomNoteListResponse(notes = responses)
    }

    @Transactional(readOnly = true)
    fun get(userId: UUID, noteId: Long): CustomNoteResponse {
        val glossary = findOwnedCustomNote(userId, noteId)
        val termCount = glossaryTermRepository.countByIdGlossaryId(noteId)
        val learnedCount = glossaryProgressRepository
            .findByIdUserIdAndIdGlossaryId(userId, noteId)
            ?.learnedCount ?: 0
        return glossary.toResponse(learnedCount = learnedCount, termCount = termCount)
    }

    @Transactional
    fun update(userId: UUID, noteId: Long, request: UpdateCustomNoteRequest): CustomNoteResponse {
        val glossary = findOwnedCustomNote(userId, noteId)

        val newTitle = request.title.trim()
        if (newTitle != glossary.title && glossaryRepository.existsByOwnerIdAndTitle(userId, newTitle)) {
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        glossary.title = newTitle
        try {
            glossaryRepository.save(glossary)
        } catch (_: DataIntegrityViolationException) {
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        val termCount = glossaryTermRepository.countByIdGlossaryId(noteId)
        val learnedCount = glossaryProgressRepository
            .findByIdUserIdAndIdGlossaryId(userId, noteId)
            ?.learnedCount ?: 0
        return glossary.toResponse(learnedCount = learnedCount, termCount = termCount)
    }

    @Transactional
    fun deleteNote(userId: UUID, noteId: Long) {
        val glossary = findOwnedCustomNote(userId, noteId)
        cascadeDeleteNote(userId, glossary.id)
    }

    @Transactional
    fun addTerms(userId: UUID, noteId: Long, termIds: List<Long>): AddTermsResponse {
        val glossary = findOwnedCustomNote(userId, noteId)

        val currentTermCount = glossaryTermRepository.countByIdGlossaryId(noteId)
        if (currentTermCount + termIds.size > MAX_TERMS_PER_NOTE) {
            throw ApiException(CustomNoteError.TERM_LIMIT_EXCEEDED)
        }

        val distinctTermIds = termIds.distinct()
        val terms = termRepository.findByIdInAndDeletedAtIsNull(distinctTermIds).associateBy { it.id }
        if (terms.size != distinctTermIds.size) {
            throw ApiException(DictionaryError.TERM_NOT_FOUND)
        }

        val existingTermIds = glossaryTermRepository
            .findAllByIdGlossaryIdAndIdTermIdIn(noteId, distinctTermIds)
            .mapTo(mutableSetOf()) { it.id.termId }

        val newPairs = distinctTermIds.filter { it !in existingTermIds }
        if (newPairs.isEmpty()) {
            return AddTermsResponse(
                added = 0,
                skipped = distinctTermIds.size,
                totalTermCount = currentTermCount
            )
        }

        val newEntities = newPairs.map { termId ->
            GlossaryTermEntity(
                id = GlossaryTermId(glossaryId = noteId, termId = termId),
                glossary = glossary,
                term = terms[termId]!!
            )
        }
        glossaryTermRepository.saveAll(newEntities)

        return AddTermsResponse(
            added = newPairs.size,
            skipped = distinctTermIds.size - newPairs.size,
            totalTermCount = currentTermCount + newPairs.size
        )
    }

    @Transactional(readOnly = true)
    fun listTerms(userId: UUID, noteId: Long, page: Int, size: Int): CustomNoteTermsResponse {
        val glossary = findOwnedCustomNote(userId, noteId)
        val pageable = PageRequest.of(page, size)
        val result = glossaryTermRepository.findPagedSimpleTermsByGlossaryId(noteId, pageable)
        return CustomNoteTermsResponse(
            noteId = glossary.id,
            title = glossary.title,
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
        findOwnedCustomNote(userId, noteId)
        val distinctTermIds = termIds.distinct()

        val existing = glossaryTermRepository
            .findAllByIdGlossaryIdAndIdTermIdIn(noteId, distinctTermIds)
            .mapTo(mutableSetOf()) { it.id.termId }

        // 단어장에 없는 termId 가 하나라도 있으면 전체 거부 (단일 삭제와 일관)
        if (existing.size != distinctTermIds.size) {
            throw ApiException(CustomNoteError.TERM_NOT_IN_NOTE)
        }

        // cascade: 단어장 한정으로 해당 단어들의 북마크/진행도 정리
        distinctTermIds.forEach { termId ->
            bookmarkRepository.deleteByIdUserIdAndIdGlossaryIdAndIdTermId(userId, noteId, termId)
            termProgressRepository.deleteByUserIdAndGlossaryIdAndTermId(userId, noteId, termId)
        }
        glossaryTermRepository.deleteAllByIdGlossaryIdAndIdTermIdIn(noteId, distinctTermIds)
    }

    @Transactional
    fun deleteNotes(userId: UUID, noteIds: List<Long>) {
        val distinctNoteIds = noteIds.distinct()
        val notes = glossaryRepository.findAllById(distinctNoteIds)

        // 입력한 ID 중 존재하지 않는 단어장이 있으면 전체 거부
        if (notes.size != distinctNoteIds.size) {
            throw ApiException(CustomNoteError.NOT_FOUND)
        }
        // 본인 소유 + CUSTOM 만 허용. 하나라도 위반이면 전체 거부 (단일 삭제와 일관)
        notes.forEach { note ->
            if (note.type != GlossaryType.CUSTOM) throw ApiException(CustomNoteError.NOT_CUSTOM_NOTE)
            if (note.ownerId != userId) throw ApiException(CustomNoteError.FORBIDDEN)
        }

        notes.forEach { cascadeDeleteNote(userId, it.id) }
    }

    @Transactional
    fun deleteTerm(userId: UUID, noteId: Long, termId: Long) {
        findOwnedCustomNote(userId, noteId)
        if (!glossaryTermRepository.existsByIdGlossaryIdAndIdTermId(noteId, termId)) {
            throw ApiException(CustomNoteError.TERM_NOT_IN_NOTE)
        }

        // cascade: 이 단어장 한정으로 단어 관련 사용자 데이터를 모두 정리
        bookmarkRepository.deleteByIdUserIdAndIdGlossaryIdAndIdTermId(userId, noteId, termId)
        termProgressRepository.deleteByUserIdAndGlossaryIdAndTermId(userId, noteId, termId)
        glossaryTermRepository.deleteByIdGlossaryIdAndIdTermId(noteId, termId)
        // 참고: GlossaryProgress.learnedCount 는 정밀 보정하지 않는다. 다음 학습 submit 때 자연 갱신.
    }

    private fun findOwnedCustomNote(userId: UUID, noteId: Long): GlossaryEntity {
        val glossary = glossaryRepository.findByIdOrNull(noteId)
            ?: throw ApiException(CustomNoteError.NOT_FOUND)
        if (glossary.type != GlossaryType.CUSTOM) {
            throw ApiException(CustomNoteError.NOT_CUSTOM_NOTE)
        }
        if (glossary.ownerId != userId) {
            throw ApiException(CustomNoteError.FORBIDDEN)
        }
        return glossary
    }

    private fun cascadeDeleteNote(userId: UUID, noteId: Long) {
        bookmarkRepository.deleteByIdUserIdAndIdGlossaryId(userId, noteId)
        termProgressRepository.deleteByUserIdAndGlossaryId(userId, noteId)
        glossaryProgressRepository.deleteByIdUserIdAndIdGlossaryId(userId, noteId)
        glossaryTermRepository.deleteByIdGlossaryId(noteId)
        glossaryRepository.deleteById(noteId)
    }

    // title 이 비어 있으면 "단어장 {N}" 으로 자동 생성. N 은 기존 자동 생성 번호의 max + 1.
    // 기존 자동 생성 단어장이 없으면 1 부터 시작.
    private fun resolveTitle(userId: UUID, title: String?): String {
        val trimmed = title?.trim()
        if (!trimmed.isNullOrEmpty()) return trimmed

        val nextNumber = glossaryRepository.findAutoGeneratedTitlesByOwner(userId)
            .mapNotNull { AUTO_TITLE_PATTERN.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull()
            ?.let { it + 1 }
            ?: 1
        return "$AUTO_TITLE_PREFIX $nextNumber"
    }

    companion object {
        private const val MAX_NOTES_PER_USER = 20L
        private const val MAX_TERMS_PER_NOTE = 250L
        private const val CUSTOM_CATEGORY = "CUSTOM"
        private const val AUTO_TITLE_PREFIX = "단어장"
        private val AUTO_TITLE_PATTERN = Regex("""^단어장 (\d+)$""")
    }
}
