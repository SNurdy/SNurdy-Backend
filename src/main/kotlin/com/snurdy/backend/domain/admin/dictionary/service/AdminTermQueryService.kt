package com.snurdy.backend.domain.admin.dictionary.service

import com.snurdy.backend.domain.admin.dictionary.dto.TermDetailResponse
import com.snurdy.backend.domain.admin.dictionary.dto.TermPageResponse
import com.snurdy.backend.domain.admin.dictionary.dto.toDetail
import com.snurdy.backend.domain.admin.dictionary.dto.toResponse
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminTermQueryService(
    private val termRepository: TermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val glossaryTermRepository: GlossaryTermRepository
) {

    @Transactional(readOnly = true)
    fun getTerms(keyword: String?, page: Int, size: Int): TermPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        val data = if (keyword.isNullOrBlank()) {
            termRepository.findByDeletedAtIsNull(pageable)
        } else {
            termRepository.findActiveByKeyword(keyword.trim(), pageable)
        }
        return data.toResponse()
    }

    @Transactional(readOnly = true)
    fun getTerm(termId: Long): TermDetailResponse {
        val term = termRepository.findByIdAndDeletedAtIsNull(termId)
            .orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }
        val glossaryIds = glossaryTermRepository.findGlossariesByTermId(termId).map { it.glossaryId }
        val morphemes = termMorphemeRepository.findOrderedMorphemesByTermId(termId)
            .map { it.toResponse() }
        return term.toDetail(glossaryIds = glossaryIds, morphemes = morphemes)
    }
}
