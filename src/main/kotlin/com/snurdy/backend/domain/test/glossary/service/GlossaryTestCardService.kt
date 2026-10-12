package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.card.dto.splitAcceptableAnswers
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.service.MorphemeHighlightAssembler
import com.snurdy.backend.domain.test.common.dto.deriveAnswer
import com.snurdy.backend.domain.test.common.dto.deriveQuestion
import com.snurdy.backend.domain.test.common.dto.isMultipleChoice
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCard
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsResponse
import com.snurdy.backend.domain.test.glossary.dto.toTestCard
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GlossaryTestCardService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val cardCountDistributor: GlossaryCardCountDistributor,
    private val cardTermDistributor: GlossaryCardTermDistributor,
    private val distractorSelector: GlossaryDistractorSelector,
    private val morphemeHighlightAssembler: MorphemeHighlightAssembler
) {

    @Transactional(readOnly = true)
    fun listTestCards(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryTestCardsRequest
    ): GlossaryTestCardsResponse {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        // 학습한 단어(termIds)로만 카드 생성 — 샘플링 없이 학습-테스트 단어 일치 보장
        val terms = glossaryTermRepository
            .findSimpleTermsByGlossaryIdAndTermIds(glossaryId, request.termIds)
        if (terms.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val slots = cardCountDistributor.groupIntoSlots(request.testTypes)
        val assignments = cardTermDistributor.distribute(terms, slots)
        if (assignments.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val distractorPool = if (assignments.any { it.second.isMultipleChoice() }) {
            glossaryTermRepository.findAllSimpleTermsByGlossaryId(glossaryId)
        } else {
            emptyList()
        }

        val termIds = terms.map { it.termId }
        val bookmarkedTermIds = bookmarkRepository
            .findBookmarkedTermIds(userId, glossaryId, termIds)
            .toSet()
        val morphemesByTermId = morphemeHighlightAssembler.buildByTermIds(
            termIds = termIds,
            spellingByTermId = terms.associate { it.termId to it.spelling }
        )

        val cards = assignments.map { (term, testType) ->
            val answer = testType.deriveAnswer(term.spelling, term.meaning, term.abbreviation)
            term.toTestCard(
                testType = testType,
                answer = answer,
                distractors = if (testType.isMultipleChoice()) {
                    distractorSelector.select(term.termId, answer, testType, distractorPool)
                } else null,
                bookmarked = term.termId in bookmarkedTermIds,
                morphemes = morphemesByTermId[term.termId].orEmpty()
            )
        }

        val availableCounts = cards
            .groupingBy { it.testType }
            .eachCount()

        return GlossaryTestCardsResponse(
            availableCounts = availableCounts,
            cards = cards.shuffled()
        )
    }
}