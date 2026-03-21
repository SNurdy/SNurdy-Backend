package com.snurdy.backend.domain.catalog.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.catalog.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.catalog.dto.ClientGlossarySummary
import com.snurdy.backend.domain.catalog.dto.ClientGlossaryTermItem
import com.snurdy.backend.domain.catalog.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.catalog.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.catalog.dto.ClientTermSummary
import com.snurdy.backend.domain.catalog.dto.TermSearchResponse
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.catalog.term.repository.TermRepository
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.global.constants.CatalogError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.math.ceil

@Service
class ClientCatalogService(
    private val catalogQueryService: CatalogQueryService,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val bookmarkRepository: BookmarkRepository
) {
    @Transactional(readOnly = true)
    fun searchTerms(userId: UUID, query: String?, page: Int, size: Int): TermSearchResponse {
        val pageable = PageRequest.of(page, size)
        val termPage = termRepository.searchActiveTerms(query, pageable)

        val termIds = termPage.content.mapNotNull { it.id }
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
                    id = term.id!!,
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
        val detail = catalogQueryService.getTermDetail(termId)
        val bookmarks = bookmarkRepository.findAllByIdUserIdAndIdTermId(userId, termId)

        return ClientTermDetailResponse(
            term = detail.term,
            glossaries = detail.glossaries,
            morphemes = detail.morphemes,
            bookmarked = bookmarks.isNotEmpty(),
            bookmarkedGlossaryIds = bookmarks.map { it.id.glossaryId }.distinct().sorted()
        )
    }

    @Transactional(readOnly = true)
    fun getGlossaries(userId: UUID, category: String?): ClientGlossaryListResponse {
        val normalizedCategory = category?.trim().orEmpty()

        val glossaryProgressMap = glossaryProgressRepository.findAllByIdUserId(userId)
            .associateBy { it.id.glossaryId }

        val glossaries = glossaryRepository.findGlossaryTermCounts()
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

                ClientGlossarySummary(
                    id = projection.glossaryId,
                    category = projection.category,
                    title = projection.title,
                    termCount = projection.termCount,
                    learnedCount = learnedCount,
                    progressRate = progressRate
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
            .orElseThrow { ApiException(CatalogError.GLOSSARY_NOT_FOUND) }

        val normalizedFilter = normalizeFilter(filter)

        val termSummaries = glossaryTermRepository.findTermSummariesByGlossaryId(glossaryId)
        val termIds = termSummaries.map { it.termId }

        val progressMap = if (termIds.isEmpty()) {
            emptyMap()
        } else {
            termProgressRepository.findByUserIdAndGlossaryIdAndTermIdIn(userId, glossaryId, termIds)
                .associateBy { it.term.id!! }
        }
        val bookmarkedTermIds = if (termIds.isEmpty()) {
            emptySet()
        } else {
            bookmarkRepository.findAllByIdUserIdAndIdTermIdIn(userId, termIds)
                .map { it.id.termId }
                .toSet()
        }

        val filteredTerms = termSummaries
            .map { projection ->
                val progress = progressMap[projection.termId]
                val status = progress?.status ?: TermProgressStatus.NEW

                ClientGlossaryTermItem(
                    id = projection.termId,
                    spelling = projection.spelling,
                    meaning = projection.meaning,
                    abbreviation = projection.abbreviation,
                    status = status.name,
                    bookmarked = projection.termId in bookmarkedTermIds
                )
            }
            .filter { item ->
                when (normalizedFilter) {
                    TermFilter.LEARNED -> item.status != TermProgressStatus.NEW.name
                    TermFilter.NOT_LEARNED -> item.status == TermProgressStatus.NEW.name
                    TermFilter.ALL -> true
                }
            }

        val safeSize = size.coerceAtLeast(1)
        val safePage = page.coerceAtLeast(0)
        val fromIndex = (safePage * safeSize).coerceAtMost(filteredTerms.size)
        val toIndex = (fromIndex + safeSize).coerceAtMost(filteredTerms.size)
        val totalElements = filteredTerms.size.toLong()
        val totalPages = if (totalElements == 0L) 0 else ceil(totalElements.toDouble() / safeSize.toDouble()).toInt()

        return ClientGlossaryTermsResponse(
            glossaryId = glossary.id!!,
            category = glossary.category,
            title = glossary.title,
            filter = normalizedFilter.responseValue,
            page = safePage,
            size = safeSize,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = toIndex < filteredTerms.size,
            terms = filteredTerms.subList(fromIndex, toIndex)
        )
    }

    private fun normalizeFilter(filter: String): TermFilter {
        val value = filter.trim().lowercase()
        return TermFilter.entries.firstOrNull { it.apiValue == value }
            ?: throw ApiException(CommonError.BAD_REQUEST, "filter는 all|learned|notLearned 중 하나여야 합니다")
    }

    private enum class TermFilter(val apiValue: String, val responseValue: String) {
        ALL("all", "all"),
        LEARNED("learned", "learned"),
        NOT_LEARNED("notlearned", "notLearned")
    }
}
