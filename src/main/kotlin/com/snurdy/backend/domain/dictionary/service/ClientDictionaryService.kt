package com.snurdy.backend.domain.dictionary.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.user.termMemo.repository.UserTermMemoRepository
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.dictionary.dto.ClientGlossarySummary
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermItem
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.dictionary.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.ClientTermSummary
import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ClientDictionaryService(
    private val dictionaryQueryService: DictionaryQueryService,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val userTermMemoRepository: UserTermMemoRepository
) {
    @Transactional(readOnly = true)
    fun searchTerms(userId: UUID, query: String?, page: Int, size: Int): TermSearchResponse {
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

        return TermSearchResponse(
            page = termPage.number,
            size = termPage.size,
            totalElements = termPage.totalElements,
            totalPages = termPage.totalPages,
            hasNext = termPage.hasNext(),
            terms = termPage.content.map { term ->
                ClientTermSummary(
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
    fun getTermDetail(userId: UUID, termId: Long): ClientTermDetailResponse {
        val detail = dictionaryQueryService.getTermDetail(termId)
        val bookmarks = bookmarkRepository.findAllByIdUserIdAndIdTermId(userId, termId)
        val memo = userTermMemoRepository.findByIdUserIdAndIdTermId(userId, termId)

        return ClientTermDetailResponse(
            term = detail.term,
            glossaries = detail.glossaries,
            morphemes = detail.morphemes,
            bookmarked = bookmarks.isNotEmpty(),
            bookmarkedGlossaryIds = bookmarks.map { it.id.glossaryId }.distinct().sorted(),
            memo = memo?.content
        )
    }

    @Transactional(readOnly = true)
    fun getGlossaries(userId: UUID, category: String?): ClientGlossaryListResponse {
        val normalizedCategory = category?.trim().orEmpty()

        val glossaryProgressMap = glossaryProgressRepository.findAllByIdUserId(userId)
            .associateBy { it.id.glossaryId }

        // 사전 카탈로그 endpoint 라 type=DEFAULT 만 노출한다. CUSTOM/WRONG_ANSWER 는 study-note 도메인 책임.
        val glossaries = glossaryRepository.findDefaultGlossaryTermCounts()
            .asSequence()
            .filter {
                normalizedCategory.isBlank() ||
                    it.category.contains(normalizedCategory, ignoreCase = true)
            }
            .map { projection ->
                val learnedCount = glossaryProgressMap[projection.glossaryId]?.learnedCount ?: 0
                val progressRate = GlossaryProgressCalculator.progressRate(learnedCount, projection.termCount)

                ClientGlossarySummary(
                    id = projection.glossaryId,
                    category = projection.category,
                    title = projection.title,
                    termCount = projection.termCount,
                    learnedCount = learnedCount,
                    progressRate = progressRate,
                    level = GlossaryProgressCalculator.levelFromRate(progressRate)
                )
            }
            .toList()

        return ClientGlossaryListResponse(glossaries = glossaries)
    }

    @Transactional(readOnly = true)
    fun getGlossaryTerms(
        userId: UUID,
        glossaryId: Long,
        filter: String,
        page: Int,
        size: Int
    ): ClientGlossaryTermsResponse {
        val glossary = glossaryRepository.findById(glossaryId)
            .orElseThrow { ApiException(DictionaryError.GLOSSARY_NOT_FOUND) }

        val normalizedFilter = normalizeFilter(filter)
        val pageable = PageRequest.of(page, size)

        val pageResult = glossaryTermRepository.findPagedClientGlossaryTerms(
            userId = userId,
            glossaryId = glossaryId,
            filter = normalizedFilter.queryValue,
            pageable = pageable
        )

        return ClientGlossaryTermsResponse(
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
                ClientGlossaryTermItem(
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
