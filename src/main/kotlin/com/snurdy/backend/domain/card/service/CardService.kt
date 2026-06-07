package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.card.dto.CustomNoteCard
import com.snurdy.backend.domain.card.dto.CustomNoteCardsResponse
import com.snurdy.backend.domain.card.dto.GlossaryCard
import com.snurdy.backend.domain.card.dto.GlossaryCardsRequest
import com.snurdy.backend.domain.card.dto.GlossaryCardsResponse
import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.card.dto.SourceGlossaryInfo
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

    // POST /v2/test/glossaries/{glossaryId}/cards
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

        val cards = buildGlossaryCards(userId, glossaryId, picked)
        return GlossaryCardsResponse(cards = cards)
    }

    // POST /v2/test/custom-notes/{customNoteId}/cards
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
        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())
        val cards = picked.map { term ->
            CustomNoteCard(
                termId = term.termId,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation,
                morphemes = buildOrderedMorphemes(term, morphemesByTermId[term.termId].orEmpty(), variantsByMorphemeId)
            )
        }
        return CustomNoteCardsResponse(cards = cards)
    }

    // POST /v2/test/wrong-answer-notes/by-period/{period}/cards
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

        val cards = buildWrongAnswerCardsByPeriod(userId, picked)
        return WrongAnswerCardsResponse(cards = cards)
    }

    // POST /v2/test/wrong-answer-notes/by-glossary/{glossaryId}/cards
    @Transactional(readOnly = true)
    fun listWrongAnswerByGlossaryCards(
        userId: UUID,
        sourceGlossaryId: Long,
        request: NoteCardsRequest
    ): WrongAnswerCardsResponse {
        val sourceGlossary = glossaryRepository.findById(sourceGlossaryId).orElseThrow {
            ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
        val picked = wrongAnswerTermRepository.findCardTermsByGlossary(
            userId = userId,
            sourceGlossaryId = sourceGlossaryId,
            requireAbbreviation = request.testType.requiresAbbreviation()
        )
        if (picked.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val termIds = picked.map { it.termId }
        val bookmarkedTermIds = bookmarkRepository
            .findBookmarkedTermIds(userId, sourceGlossaryId, termIds)
            .toSet()
        val sourceGlossaryInfo = SourceGlossaryInfo(
            glossaryId = sourceGlossary.id,
            category = sourceGlossary.category
        )
        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())
        val cards = picked.map { term ->
            WrongAnswerCard(
                termId = term.termId,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation,
                bookmarked = term.termId in bookmarkedTermIds,
                morphemes = buildOrderedMorphemes(term, morphemesByTermId[term.termId].orEmpty(), variantsByMorphemeId),
                sourceGlossary = sourceGlossaryInfo
            )
        }
        return WrongAnswerCardsResponse(cards = cards)
    }

    // 기본 단어장 카드 빌드 — path glossaryId 기준 bookmark
    private fun buildGlossaryCards(
        userId: UUID,
        glossaryId: Long,
        terms: List<SimpleTermProjection>
    ): List<GlossaryCard> {
        val termIds = terms.map { it.termId }
        val bookmarkedTermIds = bookmarkRepository
            .findBookmarkedTermIds(userId, glossaryId, termIds)
            .toSet()
        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())
        return terms.map { term ->
            GlossaryCard(
                termId = term.termId,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation,
                bookmarked = term.termId in bookmarkedTermIds,
                morphemes = buildOrderedMorphemes(term, morphemesByTermId[term.termId].orEmpty(), variantsByMorphemeId)
            )
        }
    }

    // 오답노트 by-period 카드 빌드 — 단어별 sourceGlossary 매핑 + bookmark batch pair
    private fun buildWrongAnswerCardsByPeriod(
        userId: UUID,
        terms: List<SimpleTermProjection>
    ): List<WrongAnswerCard> {
        val termIds = terms.map { it.termId }
        val sourceMap = wrongAnswerTermRepository
            .findSourceGlossaryIdsByUserAndTermIds(userId, termIds)
            .associate { it.termId to it.sourceGlossaryId }

        val sourceGlossaryIds = sourceMap.values.toSet()
        val sourceGlossaryInfoById = if (sourceGlossaryIds.isEmpty()) {
            emptyMap()
        } else {
            glossaryRepository.findAllById(sourceGlossaryIds).associate { glossary ->
                glossary.id to SourceGlossaryInfo(glossaryId = glossary.id, category = glossary.category)
            }
        }

        val bookmarkedPairs = if (sourceGlossaryIds.isEmpty()) {
            emptySet()
        } else {
            bookmarkRepository
                .findBookmarkedPairs(userId, sourceGlossaryIds, termIds)
                .mapTo(mutableSetOf()) { it.glossaryId to it.termId }
        }

        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())

        return terms.mapNotNull { term ->
            val sourceGlossaryId = sourceMap[term.termId] ?: return@mapNotNull null
            val sourceGlossaryInfo = sourceGlossaryInfoById[sourceGlossaryId] ?: return@mapNotNull null
            WrongAnswerCard(
                termId = term.termId,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation,
                bookmarked = (sourceGlossaryId to term.termId) in bookmarkedPairs,
                morphemes = buildOrderedMorphemes(term, morphemesByTermId[term.termId].orEmpty(), variantsByMorphemeId),
                sourceGlossary = sourceGlossaryInfo
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
        term: SimpleTermProjection,
        morphemes: List<BulkOrderedMorphemeProjection>,
        variantsByMorphemeId: Map<Long, List<MorphemeVariantProjection>>
    ): List<OrderedMorpheme> = morphemes.map { morpheme ->
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
}

private fun TestType.requiresAbbreviation(): Boolean = when (this) {
    TestType.WRITE_ABBREVIATION, TestType.MC_PICK_ABBR -> true
    else -> false
}
