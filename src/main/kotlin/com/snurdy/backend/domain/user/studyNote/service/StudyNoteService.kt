package com.snurdy.backend.domain.user.studyNote.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.LevelCalculator
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryType
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.user.studyNote.dto.AddTermsResult
import com.snurdy.backend.domain.user.studyNote.dto.CreateStudyNoteRequest
import com.snurdy.backend.domain.user.studyNote.dto.RemoveStudyNotesResult
import com.snurdy.backend.domain.user.studyNote.dto.RemoveTermsResult
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteListResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermItem
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermsResponse
import com.snurdy.backend.domain.user.studyNote.dto.UpdateStudyNoteRequest
import org.springframework.data.domain.PageRequest
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.StudyNoteError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StudyNoteService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val bookmarkRepository: BookmarkRepository
) {

    @Transactional
    fun create(userId: UUID, request: CreateStudyNoteRequest): StudyNoteResponse {
        if (glossaryRepository.countByOwnerIdAndType(userId, GlossaryType.CUSTOM) >= MAX_NOTES_PER_USER) {
            throw ApiException(StudyNoteError.GLOSSARY_LIMIT_EXCEEDED)
        }

        val resolvedTitle = resolveTitle(userId, request.title)

        // 사전 중복 체크 (자동 생성 경로에서는 충돌하지 않지만 안전망)
        if (glossaryRepository.existsByOwnerIdAndTitle(userId, resolvedTitle)) {
            throw ApiException(StudyNoteError.DUPLICATED_TITLE)
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
            throw ApiException(StudyNoteError.DUPLICATED_TITLE)
        }

        return toResponse(saved, learnedCount = 0, termCount = 0L)
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID): StudyNoteListResponse {
        val notes = glossaryRepository.findAllByOwnerIdAndTypeOrderByCreatedAtDesc(userId, GlossaryType.CUSTOM)
        if (notes.isEmpty()) return StudyNoteListResponse(notes = emptyList())

        val progressByGlossaryId = glossaryProgressRepository.findAllByIdUserId(userId)
            .associateBy { it.id.glossaryId }

        val responses = notes.map { glossary ->
            val termCount = glossaryTermRepository.countByIdGlossaryId(glossary.id)
            val learnedCount = progressByGlossaryId[glossary.id]?.learnedCount ?: 0
            toResponse(glossary, learnedCount = learnedCount, termCount = termCount)
        }

        return StudyNoteListResponse(notes = responses)
    }

    @Transactional(readOnly = true)
    fun get(userId: UUID, noteId: Long): StudyNoteResponse {
        val glossary = findOwnedCustomNote(userId, noteId)
        val termCount = glossaryTermRepository.countByIdGlossaryId(noteId)
        val learnedCount = glossaryProgressRepository
            .findByIdUserIdAndIdGlossaryId(userId, noteId)
            ?.learnedCount ?: 0
        return toResponse(glossary, learnedCount = learnedCount, termCount = termCount)
    }

    @Transactional
    fun update(userId: UUID, noteId: Long, request: UpdateStudyNoteRequest): StudyNoteResponse {
        val glossary = findOwnedCustomNote(userId, noteId)

        val newTitle = request.title.trim()
        if (newTitle != glossary.title && glossaryRepository.existsByOwnerIdAndTitle(userId, newTitle)) {
            throw ApiException(StudyNoteError.DUPLICATED_TITLE)
        }

        glossary.title = newTitle
        try {
            glossaryRepository.save(glossary)
        } catch (_: DataIntegrityViolationException) {
            throw ApiException(StudyNoteError.DUPLICATED_TITLE)
        }

        val termCount = glossaryTermRepository.countByIdGlossaryId(noteId)
        val learnedCount = glossaryProgressRepository
            .findByIdUserIdAndIdGlossaryId(userId, noteId)
            ?.learnedCount ?: 0
        return toResponse(glossary, learnedCount = learnedCount, termCount = termCount)
    }

    @Transactional
    fun delete(userId: UUID, noteId: Long) {
        val glossary = findOwnedCustomNote(userId, noteId)
        cascadeDeleteNote(userId, glossary.id)
    }

    @Transactional
    fun addTerms(userId: UUID, noteId: Long, termIds: List<Long>): AddTermsResult {
        val glossary = findOwnedCustomNote(userId, noteId)

        val currentTermCount = glossaryTermRepository.countByIdGlossaryId(noteId)
        if (currentTermCount + termIds.size > MAX_TERMS_PER_NOTE) {
            throw ApiException(StudyNoteError.TERM_LIMIT_EXCEEDED)
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
            return AddTermsResult(
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

        return AddTermsResult(
            added = newPairs.size,
            skipped = distinctTermIds.size - newPairs.size,
            totalTermCount = currentTermCount + newPairs.size
        )
    }

    @Transactional(readOnly = true)
    fun listTerms(userId: UUID, noteId: Long, page: Int, size: Int): StudyNoteTermsResponse {
        val glossary = findOwnedCustomNote(userId, noteId)
        val pageable = PageRequest.of(page, size)
        val result = glossaryTermRepository.findPagedSimpleTermsByGlossaryId(noteId, pageable)
        return StudyNoteTermsResponse(
            noteId = glossary.id,
            title = glossary.title,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
            terms = result.content.map {
                StudyNoteTermItem(
                    id = it.termId,
                    spelling = it.spelling,
                    meaning = it.meaning,
                    abbreviation = it.abbreviation
                )
            }
        )
    }

    @Transactional
    fun removeTerms(userId: UUID, noteId: Long, termIds: List<Long>): RemoveTermsResult {
        findOwnedCustomNote(userId, noteId)
        val distinctTermIds = termIds.distinct()

        val existing = glossaryTermRepository
            .findAllByIdGlossaryIdAndIdTermIdIn(noteId, distinctTermIds)
            .mapTo(mutableSetOf()) { it.id.termId }

        if (existing.isEmpty()) {
            val total = glossaryTermRepository.countByIdGlossaryId(noteId)
            return RemoveTermsResult(removed = 0, skipped = distinctTermIds.size, totalTermCount = total)
        }

        // cascade: 단어장 한정으로 해당 단어들의 북마크/진행도 정리
        existing.forEach { termId ->
            bookmarkRepository.deleteByIdUserIdAndIdGlossaryIdAndIdTermId(userId, noteId, termId)
            termProgressRepository.deleteByUserIdAndGlossaryIdAndTermId(userId, noteId, termId)
        }
        glossaryTermRepository.deleteAllByIdGlossaryIdAndIdTermIdIn(noteId, existing)

        val total = glossaryTermRepository.countByIdGlossaryId(noteId)
        return RemoveTermsResult(
            removed = existing.size,
            skipped = distinctTermIds.size - existing.size,
            totalTermCount = total
        )
    }

    @Transactional
    fun deleteMany(userId: UUID, noteIds: List<Long>): RemoveStudyNotesResult {
        val distinctNoteIds = noteIds.distinct()
        val notes = glossaryRepository.findAllById(distinctNoteIds)

        // 본인 소유 CUSTOM 만 대상. 그 외는 조용히 건너뛴다 (다중 삭제는 부분 성공 허용).
        val targets = notes.filter { it.type == GlossaryType.CUSTOM && it.ownerId == userId }
        targets.forEach { cascadeDeleteNote(userId, it.id) }

        return RemoveStudyNotesResult(removed = targets.size)
    }

    @Transactional
    fun removeTerm(userId: UUID, noteId: Long, termId: Long) {
        findOwnedCustomNote(userId, noteId)
        if (!glossaryTermRepository.existsByIdGlossaryIdAndIdTermId(noteId, termId)) {
            throw ApiException(DictionaryError.TERM_NOT_IN_GLOSSARY)
        }

        // cascade: 이 단어장 한정으로 단어 관련 사용자 데이터를 모두 정리
        bookmarkRepository.deleteByIdUserIdAndIdGlossaryIdAndIdTermId(userId, noteId, termId)
        termProgressRepository.deleteByUserIdAndGlossaryIdAndTermId(userId, noteId, termId)
        glossaryTermRepository.deleteByIdGlossaryIdAndIdTermId(noteId, termId)
        // 참고: GlossaryProgress.learnedCount 는 정밀 보정하지 않는다. 다음 학습 submit 때 자연 갱신.
    }

    private fun findOwnedCustomNote(userId: UUID, noteId: Long): GlossaryEntity {
        val glossary = glossaryRepository.findByIdOrNull(noteId)
            ?: throw ApiException(StudyNoteError.NOT_FOUND)
        if (glossary.type != GlossaryType.CUSTOM) {
            throw ApiException(StudyNoteError.SYSTEM_NOTE_NOT_EDITABLE)
        }
        if (glossary.ownerId != userId) {
            throw ApiException(StudyNoteError.FORBIDDEN)
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

    private fun toResponse(
        glossary: GlossaryEntity,
        learnedCount: Int,
        termCount: Long
    ): StudyNoteResponse {
        val progressRate = if (termCount == 0L) {
            0.0
        } else {
            (learnedCount.toDouble() / termCount.toDouble() * 1000.0).toInt() / 10.0
        }
        return StudyNoteResponse(
            id = glossary.id,
            title = glossary.title,
            termCount = termCount,
            learnedCount = learnedCount,
            progressRate = progressRate,
            level = LevelCalculator.fromProgressRate(progressRate),
            createdAt = glossary.createdAt
        )
    }

    companion object {
        private const val MAX_NOTES_PER_USER = 50L
        private const val MAX_TERMS_PER_NOTE = 1000L
        private const val CUSTOM_CATEGORY = "CUSTOM"
        private const val AUTO_TITLE_PREFIX = "단어장"
        private val AUTO_TITLE_PATTERN = Regex("""^단어장 (\d+)$""")
    }
}
