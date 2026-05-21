package com.snurdy.backend.domain.user.studyNote.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.service.DictionaryQueryService
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteListResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteSummary
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermDetailResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermItem
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermSearchResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermSummary
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermsResponse
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StudyNoteService(
    private val dictionaryQueryService: DictionaryQueryService,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val bookmarkRepository: BookmarkRepository
) {
    @Transactional(readOnly = true)
    fun searchTerms(userId: UUID, query: String?, page: Int, size: Int): StudyNoteTermSearchResponse {
        val pageable = PageRequest.of(page, size)
        // BOOLEAN MODE 연산자(+, -, *, " 등)를 제거해 쿼리 오류 방지
        val sanitizedQuery = query?.trim()?.replace(Regex("[+\\-><()~*\"@]+"), " ")?.trim()
        val termPage = if (sanitizedQuery.isNullOrBlank()) {
            termRepository.findByDeletedAtIsNull(pageable)
        } else {
            termRepository.searchActiveTerms(sanitizedQuery, pageable)
        }

        val termIds = termPage.content.map { it.id }
        val bookmarkedTermIds = if (termIds.isEmpty()) {
            emptySet()
        } else {
            bookmarkRepository.findAllByIdUserIdAndIdTermIdIn(userId, termIds)
                .map { it.id.termId }
                .toSet()
        }

        return StudyNoteTermSearchResponse(
            page = termPage.number,
            size = termPage.size,
            totalElements = termPage.totalElements,
            totalPages = termPage.totalPages,
            hasNext = termPage.hasNext(),
            terms = termPage.content.map { term ->
                StudyNoteTermSummary(
                    id = term.id,
                    spelling = term.spelling,
                    meaning = term.meaning,
                    abbreviation = term.abbreviation,
                    bookmarked = term.id in bookmarkedTermIds
                )
            }
        )
    }

    @Transactional(readOnly = true)
    fun getTermDetail(userId: UUID, termId: Long): StudyNoteTermDetailResponse {
        val detail = dictionaryQueryService.getTermDetail(termId)
        val bookmarks = bookmarkRepository.findAllByIdUserIdAndIdTermId(userId, termId)

        return StudyNoteTermDetailResponse(
            term = detail.term,
            glossaries = detail.glossaries,
            morphemes = detail.morphemes,
            bookmarked = bookmarks.isNotEmpty(),
            bookmarkedGlossaryIds = bookmarks.map { it.id.glossaryId }.distinct().sorted()
        )
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, category: String?): StudyNoteListResponse {
        val normalizedCategory = category?.trim().orEmpty()

        val glossaryProgressMap = glossaryProgressRepository.findAllByIdUserId(userId)
            .associateBy { it.id.glossaryId }

        val notes = glossaryRepository.findGlossaryTermCounts()
            .asSequence()
            .filter {
                normalizedCategory.isBlank() ||
                    it.category.contains(normalizedCategory, ignoreCase = true)
            }
            .map { projection ->
                val learnedCount = glossaryProgressMap[projection.glossaryId]?.learnedCount ?: 0
                val progressRate = if (projection.termCount == 0L) {
                    0.0
                } else {
                    ((learnedCount.toDouble() / projection.termCount.toDouble()) * 1000.0)
                        .toInt() / 10.0
                }

                StudyNoteSummary(
                    id = projection.glossaryId,
                    category = projection.category,
                    title = projection.title,
                    termCount = projection.termCount,
                    learnedCount = learnedCount,
                    progressRate = progressRate
                )
            }
            .toList()

        return StudyNoteListResponse(notes = notes)
    }

    @Transactional(readOnly = true)
    fun getTerms(
        userId: UUID,
        glossaryId: Long,
        filter: String,
        page: Int,
        size: Int
    ): StudyNoteTermsResponse {
        val glossary = glossaryRepository.findById(glossaryId)
            .orElseThrow { ApiException(DictionaryError.GLOSSARY_NOT_FOUND) }

        val normalizedFilter = normalizeFilter(filter)
        val pageable = PageRequest.of(page, size)

        val pageResult = glossaryTermRepository.findPagedStudyNoteTerms(
            userId = userId,
            glossaryId = glossaryId,
            filter = normalizedFilter.queryValue,
            pageable = pageable
        )

        return StudyNoteTermsResponse(
            glossaryId = glossary.id,
            category = glossary.category,
            title = glossary.title,
            filter = normalizedFilter.responseValue,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext(),
            terms = pageResult.content.map { projection ->
                StudyNoteTermItem(
                    id = projection.termId,
                    spelling = projection.spelling,
                    meaning = projection.meaning,
                    abbreviation = projection.abbreviation,
                    status = projection.status,
                    bookmarked = projection.bookmarked == 1
                )
            }
        )
    }

    private fun normalizeFilter(filter: String): TermFilter {
        val value = filter.trim().lowercase()
        return TermFilter.entries.firstOrNull { it.apiValue == value }
            ?: throw ApiException(DictionaryError.INVALID_FILTER)
    }

    private enum class TermFilter(val apiValue: String, val responseValue: String, val queryValue: String) {
        ALL("all", "all", "ALL"),
        LEARNED("learned", "learned", "LEARNED"),
        NOT_LEARNED("notlearned", "notLearned", "NOT_LEARNED")
    }
}
