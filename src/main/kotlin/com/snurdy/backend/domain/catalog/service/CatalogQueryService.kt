package com.snurdy.backend.domain.catalog.service

import com.snurdy.backend.domain.catalog.dto.GlossaryListResponse
import com.snurdy.backend.domain.catalog.dto.GlossarySummary
import com.snurdy.backend.domain.catalog.dto.GlossaryTermsResponse
import com.snurdy.backend.domain.catalog.dto.MorphemeHighlight
import com.snurdy.backend.domain.catalog.dto.OrderedMorpheme
import com.snurdy.backend.domain.catalog.dto.TermDetail
import com.snurdy.backend.domain.catalog.dto.TermDetailResponse
import com.snurdy.backend.domain.catalog.dto.TermGlossaryTag
import com.snurdy.backend.domain.catalog.dto.TermSummary
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.catalog.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.domain.catalog.morpheme.dto.MorphemeSearchTermItem
import com.snurdy.backend.domain.catalog.morpheme.dto.SearchOrderedMorpheme
import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.domain.catalog.morpheme.service.HighlightCalculator
import com.snurdy.backend.domain.catalog.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.catalog.term.repository.TermRepository
import com.snurdy.backend.global.constants.CatalogError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class CatalogQueryService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val highlightCalculator: HighlightCalculator

) {

    fun getGlossaries(): GlossaryListResponse {
        val glossaries = glossaryRepository.findGlossaryTermCounts()
            .map {
                GlossarySummary(
                    id = it.glossaryId,
                    category = it.category,
                    title = it.title,
                    termCount = it.termCount
                )
            }

        return GlossaryListResponse(glossaries = glossaries)
    }

    fun getGlossaryTerms(glossaryId: Long, page: Int, size: Int): GlossaryTermsResponse {
        val glossary = glossaryRepository.findById(glossaryId)
            .orElseThrow { ApiException(CatalogError.GLOSSARY_NOT_FOUND) }

        val pageable = PageRequest.of(page, size)
        val termPage = glossaryTermRepository.findPagedTermSummariesByGlossaryId(glossaryId, pageable)

        return GlossaryTermsResponse(
            glossaryId = glossary.id!!,
            category = glossary.category,
            title = glossary.title,
            page = termPage.number,
            size = termPage.size,
            totalElements = termPage.totalElements,
            totalPages = termPage.totalPages,
            hasNext = termPage.hasNext(),
            terms = termPage.content.map {
                TermSummary(
                    id = it.termId,
                    spelling = it.spelling,
                    meaning = it.meaning,
                    abbreviation = it.abbreviation
                )
            }
        )
    }

    fun getTermDetail(termId: Long): TermDetailResponse {
        val term = termRepository.findById(termId)
            .orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
        val glossaries = glossaryTermRepository.findGlossariesByTermId(termId)
            .map {
                TermGlossaryTag(
                    id = it.glossaryId,
                    category = it.category,
                    title = it.title
                )
            }

        val morphemes = termMorphemeRepository.findOrderedMorphemesByTermId(termId)

        // 어근어미 ID 목록으로 variants 일괄 조회
        val morphemeIds = morphemes.map { it.morphemeId }
        val variantsByMorphemeId = if (morphemeIds.isNotEmpty()) {
            morphemeVariantRepository.findByMorphemeIds(morphemeIds)
                .groupBy { it.morphemeId }
        } else {
            emptyMap()
        }

        // Fallback 매칭으로 highlight 범위 계산
        val orderedMorphemes = morphemes.map { m ->
            val variants = variantsByMorphemeId[m.morphemeId] ?: emptyList()
            val range = highlightCalculator.calculate(term.spelling, variants)

            OrderedMorpheme(
                id = m.morphemeId,
                spelling = m.spelling,
                meaning = m.meaning,
                order = m.position,
                highlight = range?.let { MorphemeHighlight(start = it.start, length = it.length) }
            )
        }

        return TermDetailResponse(
            term = TermDetail(
                id = term.id!!,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation
            ),
            glossaries = glossaries,
            morphemes = orderedMorphemes
        )
    }

    fun searchTermsByMorpheme(keyword: String): MorphemeSearchResponse {
        val normalizedKeyword = keyword.lowercase().trim()
            .replace("-", "")
            .replace("%", "")
            .replace("_", "")
        if (normalizedKeyword.length < 2) {
            return MorphemeSearchResponse(keyword = keyword, totalCount = 0, terms = emptyList())
        }

        // 1단계: keyword prefix match → 매칭 term_id + morpheme_id 쌍
        val searchHits = morphemeVariantRepository.findTermIdsByVariantPrefix(normalizedKeyword)
        if (searchHits.isEmpty()) {
            return MorphemeSearchResponse(keyword = keyword, totalCount = 0, terms = emptyList())
        }

        val termIds = searchHits.map { it.termId }.distinct()
        val matchedMorphemeIdsByTermId = searchHits
            .groupBy({ it.termId }, { it.morphemeId })
            .mapValues { it.value.toSet() }

        // 2단계: term 기본 정보
        val termEntities = termRepository.findAllById(termIds).associateBy { it.id!! }

        // 전체 어근어미 구성 일괄
        val allMorphemes = termMorphemeRepository.findOrderedMorphemesByTermIds(termIds)
        val morphemesByTermId = allMorphemes.groupBy { it.termId }

        // highlight용 variant 일괄
        val allMorphemeIds = allMorphemes.map { it.morphemeId }.distinct()
        val variantsByMorphemeId = if (allMorphemeIds.isNotEmpty()) {
            morphemeVariantRepository.findByMorphemeIds(allMorphemeIds).groupBy { it.morphemeId }
        } else {
            emptyMap()
        }

        // 3단계: glossary 태그 일괄
        val glossaryProjections = glossaryTermRepository.findGlossariesByTermIds(termIds)
        val glossariesByTermId = glossaryProjections.groupBy { it.termId }

        // 조합
        val resultTerms = termIds.mapNotNull { termId ->
            val term = termEntities[termId] ?: return@mapNotNull null
            val matchedMorphemeIds = matchedMorphemeIdsByTermId[termId] ?: emptySet()
            val morphemes = morphemesByTermId[termId] ?: emptyList()

            val orderedMorphemes = morphemes.map { m ->
                val variants = variantsByMorphemeId[m.morphemeId] ?: emptyList()
                val range = highlightCalculator.calculate(term.spelling, variants)

                SearchOrderedMorpheme(
                    id = m.morphemeId,
                    spelling = m.spelling,
                    meaning = m.meaning,
                    order = m.position,
                    highlight = range?.let { MorphemeHighlight(start = it.start, length = it.length) },
                    matched = m.morphemeId in matchedMorphemeIds
                )
            }

            val glossaries = (glossariesByTermId[termId] ?: emptyList()).map {
                TermGlossaryTag(id = it.glossaryId, category = it.category, title = it.title)
            }

            MorphemeSearchTermItem(
                id = term.id!!,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                glossaries = glossaries,
                morphemes = orderedMorphemes
            )
        }

        return MorphemeSearchResponse(
            keyword = keyword,
            totalCount = resultTerms.size,
            terms = resultTerms
        )
    }

}
