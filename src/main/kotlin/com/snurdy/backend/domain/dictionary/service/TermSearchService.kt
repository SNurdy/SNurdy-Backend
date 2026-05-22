package com.snurdy.backend.domain.dictionary.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.dto.ClientTermSummary
import com.snurdy.backend.domain.dictionary.dto.TermGlossaryTag
import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeSearchTermItem
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// 사전 단어 검색 책임 전담
// 일반 keyword 검색 + 어근/어미 prefix 검색 두 가지 진입점 제공
@Service
class TermSearchService(
    private val termRepository: TermRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val bookmarkRepository: BookmarkRepository
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
    fun searchTermsByMorpheme(keyword: String): MorphemeSearchResponse {
        val normalizedKeyword = keyword.lowercase().trim()
            .replace("-", "")
            .replace("%", "")
            .replace("_", "")
        if (normalizedKeyword.length < MORPHEME_SEARCH_MIN_LENGTH) {
            return MorphemeSearchResponse(keyword = keyword, totalCount = 0, terms = emptyList())
        }

        val searchHits = morphemeVariantRepository.findTermIdsByVariantPrefix(
            normalizedKeyword,
            MORPHEME_SEARCH_MAX_RESULTS
        )
        if (searchHits.isEmpty()) {
            return MorphemeSearchResponse(keyword = keyword, totalCount = 0, terms = emptyList())
        }

        val termIds = searchHits.map { it.termId }.distinct()
        val termEntities = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id }
        val glossariesByTermId = glossaryTermRepository.findGlossariesByTermIds(termIds)
            .groupBy { it.termId }

        val resultTerms = termIds.mapNotNull { termId ->
            val term = termEntities[termId] ?: return@mapNotNull null
            val glossaries = (glossariesByTermId[termId] ?: emptyList()).map {
                TermGlossaryTag(id = it.glossaryId, category = it.category, title = it.title)
            }
            MorphemeSearchTermItem(
                id = term.id,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                glossaries = glossaries
            )
        }

        return MorphemeSearchResponse(
            keyword = keyword,
            totalCount = resultTerms.size,
            terms = resultTerms
        )
    }

    companion object {
        private const val MORPHEME_SEARCH_MAX_RESULTS = 200
        private const val MORPHEME_SEARCH_MIN_LENGTH = 2
    }
}
