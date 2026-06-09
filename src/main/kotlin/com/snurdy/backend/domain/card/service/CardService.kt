package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.card.dto.CustomNoteCard
import com.snurdy.backend.domain.card.dto.CustomNoteCardsResponse
import com.snurdy.backend.domain.card.dto.GlossaryCard
import com.snurdy.backend.domain.card.dto.GlossaryCardsRequest
import com.snurdy.backend.domain.card.dto.GlossaryCardsResponse
import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.card.dto.WrongAnswerCard
import com.snurdy.backend.domain.card.dto.WrongAnswerCardsResponse
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
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteTermRepository
import com.snurdy.backend.domain.user.customNote.repository.NoteCardTermProjection
import com.snurdy.backend.domain.user.customNote.service.CustomNoteService
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriod
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.thresholdAfter
import com.snurdy.backend.domain.user.wrongAnswerNote.repository.WrongAnswerTermRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class CardService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val highlightCalculator: HighlightCalculator,
    private val bookmarkRepository: BookmarkRepository,
    private val customNoteService: CustomNoteService,
    private val customNoteTermRepository: CustomNoteTermRepository,
    private val wrongAnswerTermRepository: WrongAnswerTermRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun listGlossaryCards(userId: UUID, glossaryId: Long, request: GlossaryCardsRequest): GlossaryCardsResponse {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val picked = glossaryTermRepository.pickCardTermsForTest(
            userId = userId,
            glossaryId = glossaryId,
            requireAbbreviation = request.testType.requiresAbbreviation(),
            count = request.count
        )
        if (picked.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val termIds = picked.map { it.termId }
        val bookmarkedTermIds = bookmarkRepository
            .findBookmarkedTermIds(userId, glossaryId, termIds)
            .toSet()
        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())
        val cards = picked.map { term ->
            GlossaryCard(
                termId = term.termId,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation,
                bookmarked = term.termId in bookmarkedTermIds,
                morphemes = buildOrderedMorphemes(term.spelling, morphemesByTermId[term.termId].orEmpty(), variantsByMorphemeId)
            )
        }
        return GlossaryCardsResponse(cards = cards)
    }

    @Transactional(readOnly = true)
    fun listCustomNoteCards(
        userId: UUID,
        customNoteId: Long,
        request: NoteCardsRequest
    ): CustomNoteCardsResponse {
        customNoteService.requireNoteOwnership(userId, customNoteId)

        val picked = customNoteTermRepository.findCardTermsByNoteId(
            noteId = customNoteId,
            requireAbbreviation = request.testType.requiresAbbreviation()
        )
        if (picked.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val termIds = picked.map { it.termId }
        val glossaryIds = picked.mapNotNull { it.glossaryId.takeIf { id -> id > 0 } }.toSet()

        val bookmarkedPairs = if (glossaryIds.isEmpty()) {
            emptySet()
        } else {
            bookmarkRepository
                .findBookmarkedPairs(userId, glossaryIds, termIds)
                .mapTo(mutableSetOf()) { it.glossaryId to it.termId }
        }

        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())
        val cards = picked.map { term ->
            CustomNoteCard(
                termId = term.termId,
                glossaryId = term.glossaryId,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation,
                bookmarked = (term.glossaryId to term.termId) in bookmarkedPairs,
                morphemes = buildOrderedMorphemes(term.spelling, morphemesByTermId[term.termId].orEmpty(), variantsByMorphemeId)
            )
        }
        return CustomNoteCardsResponse(cards = cards)
    }

    @Transactional(readOnly = true)
    fun listWrongAnswerByPeriodCards(
        userId: UUID,
        period: WrongAnswerPeriod,
        request: NoteCardsRequest
    ): WrongAnswerCardsResponse {
        val after = period.thresholdAfter(clock.instant())
        val picked = wrongAnswerTermRepository.findCardTermsByPeriod(
            userId = userId,
            after = after,
            requireAbbreviation = request.testType.requiresAbbreviation()
        )
        if (picked.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        return WrongAnswerCardsResponse(cards = buildWrongAnswerCards(userId, picked))
    }

    @Transactional(readOnly = true)
    fun listWrongAnswerByGlossaryCards(
        userId: UUID,
        glossaryId: Long,
        request: NoteCardsRequest
    ): WrongAnswerCardsResponse {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
        val picked = wrongAnswerTermRepository.findCardTermsByGlossary(
            userId = userId,
            glossaryId = glossaryId,
            requireAbbreviation = request.testType.requiresAbbreviation()
        )
        if (picked.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        return WrongAnswerCardsResponse(cards = buildWrongAnswerCards(userId, picked))
    }

    // 오답노트 카드 빌드 — 단어별 glossaryId + bookmark batch pair (by-period, by-glossary 공용)
    private fun buildWrongAnswerCards(
        userId: UUID,
        terms: List<NoteCardTermProjection>
    ): List<WrongAnswerCard> {
        val termIds = terms.map { it.termId }
        val glossaryIds = terms.mapNotNull { it.glossaryId.takeIf { id -> id > 0 } }.toSet()

        val bookmarkedPairs = if (glossaryIds.isEmpty()) {
            emptySet()
        } else {
            bookmarkRepository
                .findBookmarkedPairs(userId, glossaryIds, termIds)
                .mapTo(mutableSetOf()) { it.glossaryId to it.termId }
        }

        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())

        return terms.map { term ->
            WrongAnswerCard(
                termId = term.termId,
                glossaryId = term.glossaryId,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation,
                bookmarked = (term.glossaryId to term.termId) in bookmarkedPairs,
                morphemes = buildOrderedMorphemes(term.spelling, morphemesByTermId[term.termId].orEmpty(), variantsByMorphemeId)
            )
        }
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

    private fun buildOrderedMorphemes(
        spelling: String,
        morphemes: List<BulkOrderedMorphemeProjection>,
        variantsByMorphemeId: Map<Long, List<MorphemeVariantProjection>>
    ): List<OrderedMorpheme> = morphemes.map { morpheme ->
        val variants = variantsByMorphemeId[morpheme.morphemeId].orEmpty()
        val range = highlightCalculator.calculate(spelling, variants)
        OrderedMorpheme(
            id = morpheme.morphemeId,
            spelling = morpheme.spelling,
            meaning = morpheme.meaning,
            order = morpheme.position,
            highlight = range?.let { MorphemeHighlight(start = it.start, length = it.length) }
        )
    }

}

private fun TestType.requiresAbbreviation(): Boolean = when (this) {
    TestType.WRITE_ABBREVIATION, TestType.MC_PICK_ABBR -> true
    else -> false
}
