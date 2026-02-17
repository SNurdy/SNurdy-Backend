package com.snurdy.backend.domain.catalog.service

import com.snurdy.backend.domain.catalog.dto.GlossaryListResponse
import com.snurdy.backend.domain.catalog.dto.GlossarySummary
import com.snurdy.backend.domain.catalog.dto.GlossaryTermsResponse
import com.snurdy.backend.domain.catalog.dto.OrderedMorpheme
import com.snurdy.backend.domain.catalog.dto.TermDetail
import com.snurdy.backend.domain.catalog.dto.TermDetailResponse
import com.snurdy.backend.domain.catalog.dto.TermGlossaryTag
import com.snurdy.backend.domain.catalog.dto.TermSummary
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryTermRepository
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
    private val termMorphemeRepository: TermMorphemeRepository
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
            .map {
                OrderedMorpheme(
                    id = it.morphemeId,
                    spelling = it.spelling,
                    meaning = it.meaning,
                    order = it.position
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
            morphemes = morphemes
        )
    }
}
