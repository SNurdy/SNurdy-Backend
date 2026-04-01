package com.snurdy.backend.domain.admin.dictionary.service

import com.snurdy.backend.domain.admin.dictionary.dto.*
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminDictionaryQueryService(
    private val glossaryRepository: GlossaryRepository,
    private val termRepository: TermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeRepository: MorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val glossaryTermRepository: GlossaryTermRepository
) {

    @Transactional(readOnly = true)
    fun getGlossaries(keyword: String?, page: Int, size: Int): GlossaryPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        val data = if (keyword.isNullOrBlank()) {
            glossaryRepository.findAll(pageable)
        } else {
            glossaryRepository.findByCategoryContainingIgnoreCaseOrTitleContainingIgnoreCase(keyword.trim(), keyword.trim(), pageable)
        }

        return GlossaryPageResponse(
            page = data.number,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages,
            items = data.content.map { GlossaryItem(id = it.id!!, category = it.category, title = it.title) }
        )
    }

    @Transactional(readOnly = true)
    fun getTerms(keyword: String?, page: Int, size: Int): TermPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        val data = if (keyword.isNullOrBlank()) {
            termRepository.findAll(pageable)
        } else {
            val q = keyword.trim()
            termRepository.findBySpellingContainingIgnoreCaseOrMeaningContainingIgnoreCaseOrAbbreviationContainingIgnoreCase(
                q, q, q, pageable
            )
        }

        return TermPageResponse(
            page = data.number,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages,
            items = data.content.map {
                TermItem(
                    id = it.id!!,
                    spelling = it.spelling,
                    meaning = it.meaning,
                    abbreviation = it.abbreviation,
                    explanation = it.explanation
                )
            }
        )
    }

    @Transactional(readOnly = true)
    fun getTerm(termId: Long): TermDetailResponse {
        val term = termRepository.findById(termId).orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }
        val glossaryIds = glossaryTermRepository.findGlossariesByTermId(termId).map { it.glossaryId }
        val morphemes = termMorphemeRepository.findOrderedMorphemesByTermId(termId).map {
            TermMorphemeMappingResponse(
                morphemeId = it.morphemeId,
                spelling = it.spelling,
                meaning = it.meaning,
                position = it.position,
            )
        }
        return TermDetailResponse(
            id = term.id!!,
            spelling = term.spelling,
            meaning = term.meaning,
            abbreviation = term.abbreviation,
            explanation = term.explanation,
            glossaryIds = glossaryIds,
            morphemes = morphemes
        )
    }

    @Transactional(readOnly = true)
    fun getMorphemes(keyword: String?, page: Int, size: Int): MorphemePageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        val data = if (keyword.isNullOrBlank()) {
            morphemeRepository.findAll(pageable)
        } else {
            val q = keyword.trim()
            morphemeRepository.findBySpellingContainingIgnoreCaseOrMeaningContainingIgnoreCase(q, q, pageable)
        }

        return MorphemePageResponse(
            page = data.number,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages,
            items = data.content.map { MorphemeItem(id = it.id!!, spelling = it.spelling, meaning = it.meaning) }
        )
    }

    @Transactional(readOnly = true)
    fun getMorphemeVariants(keyword: String?, morphemeId: Long?, page: Int, size: Int): MorphemeVariantPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        val data = when {
            morphemeId != null -> morphemeVariantRepository.findByMorphemeId(morphemeId, pageable)
            keyword.isNullOrBlank() -> morphemeVariantRepository.findAll(pageable)
            else -> {
                val q = keyword.trim()
                morphemeVariantRepository.findByVariantContainingIgnoreCaseOrBaseContainingIgnoreCase(q, q, pageable)
            }
        }

        return MorphemeVariantPageResponse(
            page = data.number,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages,
            items = data.content.map {
                MorphemeVariantItem(
                    id = it.id,
                    morphemeId = it.morpheme.id!!,
                    variant = it.variant,
                    base = it.base
                )
            }
        )
    }
}
