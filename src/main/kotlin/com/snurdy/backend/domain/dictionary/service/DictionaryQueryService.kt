package com.snurdy.backend.domain.dictionary.service

import com.snurdy.backend.domain.dictionary.dto.MorphemeHighlight
import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.dto.TermDetail
import com.snurdy.backend.domain.dictionary.dto.TermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.TermGlossaryTag
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeSearchResponse
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeSearchTermItem
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.domain.dictionary.morpheme.service.HighlightCalculator
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DictionaryQueryService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val highlightCalculator: HighlightCalculator
) {

    companion object {
        private const val MAX_SEARCH_RESULTS = 200
    }

    fun getTermDetail(termId: Long): TermDetailResponse {
        val term = termRepository.findByIdAndDeletedAtIsNull(termId)
            .orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }
        val glossaries = glossaryTermRepository.findGlossariesByTermId(termId)
            .map {
                TermGlossaryTag(
                    id = it.glossaryId,
                    category = it.category,
                    title = it.title
                )
            }

        val morphemes = termMorphemeRepository.findOrderedMorphemesByTermId(termId)

        val morphemeIds = morphemes.map { it.morphemeId }
        val variantsByMorphemeId = if (morphemeIds.isNotEmpty()) {
            morphemeVariantRepository.findByMorphemeIds(morphemeIds)
                .groupBy { it.morphemeId }
        } else {
            emptyMap()
        }

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
                id = term.id,
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

        val searchHits = morphemeVariantRepository.findTermIdsByVariantPrefix(
            normalizedKeyword,
            MAX_SEARCH_RESULTS
        )
        if (searchHits.isEmpty()) {
            return MorphemeSearchResponse(keyword = keyword, totalCount = 0, terms = emptyList())
        }

        val termIds = searchHits.map { it.termId }.distinct()
        val termEntities = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id }
        val glossaryProjections = glossaryTermRepository.findGlossariesByTermIds(termIds)
        val glossariesByTermId = glossaryProjections.groupBy { it.termId }

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

}
