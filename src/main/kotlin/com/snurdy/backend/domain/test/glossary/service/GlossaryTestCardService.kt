package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.test.common.dto.TestType
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsResponse
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
    private val cardCountDistributor: GlossaryCardCountDistributor,
    private val cardTermDistributor: GlossaryCardTermDistributor,
    private val cardAssembler: GlossaryTestCardAssembler
) {

    @Transactional(readOnly = true)
    fun listTestCards(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryTestCardsRequest
    ): GlossaryTestCardsResponse {
        requireGlossaryExists(glossaryId)
        val terms = findStudiedTerms(glossaryId, request.termIds)
        val assignments = assignTestTypes(terms, request.testTypes)
        val cards = cardAssembler.assembleCards(userId, glossaryId, assignments)

        return GlossaryTestCardsResponse(
            availableCounts = cards.groupingBy { it.testType }.eachCount(),
            cards = cards.shuffled()
        )
    }

    private fun requireGlossaryExists(glossaryId: Long) {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
    }

    // 샘플링 없이 학습한 단어로만 카드를 만들어 학습 단어와 테스트 단어를 일치시킨다
    private fun findStudiedTerms(glossaryId: Long, termIds: List<Long>): List<SimpleTermProjection> {
        val terms = glossaryTermRepository.findSimpleTermsByGlossaryIdAndTermIds(glossaryId, termIds)
        if (terms.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
        return terms
    }

    private fun assignTestTypes(
        terms: List<SimpleTermProjection>,
        testTypes: List<TestType>
    ): List<Pair<SimpleTermProjection, TestType>> {
        val slots = cardCountDistributor.groupIntoSlots(testTypes)
        val assignments = cardTermDistributor.distribute(terms, slots)
        if (assignments.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
        return assignments
    }
}