package com.snurdy.backend.domain.study.glossary.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.service.MorphemeHighlightAssembler
import com.snurdy.backend.domain.study.glossary.dto.GlossaryStudyCardsRequest
import com.snurdy.backend.domain.study.glossary.dto.GlossaryStudyCardsResponse
import com.snurdy.backend.domain.study.glossary.dto.toStudyTerm
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GlossaryStudyService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val morphemeHighlightAssembler: MorphemeHighlightAssembler
) {

    @Transactional(readOnly = true)
    fun listStudyCards(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryStudyCardsRequest
    ): GlossaryStudyCardsResponse {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val sampledTerms = glossaryTermRepository.pickCardTermsForTest(
            userId = userId,
            glossaryId = glossaryId,
            count = request.count
        )

        val termIds = sampledTerms.map { it.termId }
        val bookmarkedTermIds = bookmarkRepository
            .findBookmarkedTermIds(userId, glossaryId, termIds)
            .toSet()
        val morphemesByTermId = morphemeHighlightAssembler.buildByTermIds(
            termIds = termIds,
            spellingByTermId = sampledTerms.associate { it.termId to it.spelling }
        )

        val terms = sampledTerms.map { term ->
            term.toStudyTerm(
                bookmarked = term.termId in bookmarkedTermIds,
                morphemes = morphemesByTermId[term.termId].orEmpty()
            )
        }

        return GlossaryStudyCardsResponse(
            requestedCount = request.count,
            terms = terms
        )
    }
}