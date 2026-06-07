package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.card.dto.Card
import com.snurdy.backend.domain.card.dto.CardsRequest
import com.snurdy.backend.domain.card.dto.CardsResponse
import com.snurdy.backend.domain.dictionary.dto.MorphemeHighlight
import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantProjection
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.domain.dictionary.morpheme.service.HighlightCalculator
import com.snurdy.backend.domain.dictionary.term.repository.BulkOrderedMorphemeProjection
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CardService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val highlightCalculator: HighlightCalculator,
    private val bookmarkRepository: BookmarkRepository
) {

    @Transactional(readOnly = true)
    fun listCards(userId: UUID, request: CardsRequest): CardsResponse {
        if (!glossaryRepository.existsById(request.glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val picked = glossaryTermRepository.pickCardTermsForTest(
            userId = userId,
            glossaryId = request.glossaryId,
            requireAbbreviation = request.testType.requiresAbbreviation(),
            count = request.count
        )
        if (picked.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val termIds = picked.map { it.termId }
        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())
        val bookmarkedTermIds = bookmarkRepository
            .findBookmarkedTermIds(userId, request.glossaryId, termIds)
            .toSet()

        val cards = picked.map { term ->
            buildCard(
                term = term,
                bookmarked = term.termId in bookmarkedTermIds,
                morphemes = morphemesByTermId[term.termId].orEmpty(),
                variantsByMorphemeId = variantsByMorphemeId
            )
        }
        return CardsResponse(glossaryId = request.glossaryId, cards = cards)
    }

    private fun fetchMorphemesByTermId(termIds: List<Long>): Map<Long, List<BulkOrderedMorphemeProjection>> {
        if (termIds.isEmpty()) return emptyMap()
        return termMorphemeRepository
            .findOrderedMorphemesByTermIds(termIds)
            .groupBy { it.termId }
    }

    private fun fetchVariantsByMorphemeId(
        morphemes: Collection<BulkOrderedMorphemeProjection>
    ): Map<Long, List<MorphemeVariantProjection>> {
        val morphemeIds = morphemes.map { it.morphemeId }.distinct()
        if (morphemeIds.isEmpty()) return emptyMap()
        return morphemeVariantRepository
            .findByMorphemeIds(morphemeIds)
            .groupBy { it.morphemeId }
    }

    private fun buildCard(
        term: SimpleTermProjection,
        bookmarked: Boolean,
        morphemes: List<BulkOrderedMorphemeProjection>,
        variantsByMorphemeId: Map<Long, List<MorphemeVariantProjection>>
    ): Card {
        val orderedMorphemes = morphemes.map { morpheme ->
            val variants = variantsByMorphemeId[morpheme.morphemeId].orEmpty()
            val range = highlightCalculator.calculate(term.spelling, variants)
            OrderedMorpheme(
                id = morpheme.morphemeId,
                spelling = morpheme.spelling,
                meaning = morpheme.meaning,
                order = morpheme.position,
                highlight = range?.let { MorphemeHighlight(start = it.start, length = it.length) }
            )
        }
        return Card(
            termId = term.termId,
            spelling = term.spelling,
            meaning = term.meaning,
            abbreviation = term.abbreviation,
            explanation = term.explanation,
            bookmarked = bookmarked,
            morphemes = orderedMorphemes
        )
    }
}

private fun TestType.requiresAbbreviation(): Boolean = when (this) {
    TestType.WRITE_ABBREVIATION, TestType.MC_PICK_ABBR -> true
    else -> false
}
