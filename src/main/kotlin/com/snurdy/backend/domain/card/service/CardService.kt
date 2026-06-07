package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.card.dto.Card
import com.snurdy.backend.domain.card.dto.CardsRequest
import com.snurdy.backend.domain.card.dto.CardsResponse
import com.snurdy.backend.domain.card.dto.CustomNoteCard
import com.snurdy.backend.domain.card.dto.CustomNoteCardsResponse
import com.snurdy.backend.domain.card.dto.GlossaryCardsRequest
import com.snurdy.backend.domain.card.dto.GlossaryCardsResponse
import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.card.dto.SourceGlossaryInfo
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

    // Phase 2.8 호환용 endpoint /v2/cards 처리. step 12 에서 폐기 예정
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
        val bookmarkedTermIds = bookmarkRepository
            .findBookmarkedTermIds(userId, request.glossaryId, termIds)
            .toSet()
        val cards = picked.map {
            buildCard(
                term = it,
                bookmarked = it.termId in bookmarkedTermIds,
                sourceGlossary = null,
                termIds = termIds
            )
        }
        return CardsResponse(glossaryId = request.glossaryId, cards = cards)
    }

    // POST /v2/test/glossaries/{glossaryId}/cards — 기본 단어장 sub-resource
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
        val cards = buildCards(
            terms = picked,
            bookmarkedBy = { it.termId in bookmarkedTermIds },
            sourceGlossaryBy = { null }
        )
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
            buildCustomNoteCard(
                term = term,
                morphemes = morphemesByTermId[term.termId].orEmpty(),
                variantsByMorphemeId = variantsByMorphemeId
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

        val cards = assembleWrongAnswerCards(userId, picked)
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
        val cards = buildCards(
            terms = picked,
            bookmarkedBy = { it.termId in bookmarkedTermIds },
            sourceGlossaryBy = { sourceGlossaryInfo }
        )
        return WrongAnswerCardsResponse(cards = cards)
    }

    // 오답노트 by-period 의 sourceGlossary 매핑 + bookmark batch 조립
    private fun assembleWrongAnswerCards(
        userId: UUID,
        terms: List<SimpleTermProjection>
    ): List<Card> {
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

        return buildCards(
            terms = terms,
            bookmarkedBy = { term ->
                val glossaryId = sourceMap[term.termId] ?: return@buildCards false
                (glossaryId to term.termId) in bookmarkedPairs
            },
            sourceGlossaryBy = { term -> sourceMap[term.termId]?.let { sourceGlossaryInfoById[it] } }
        )
    }

    // Card 리스트 조립 — morphemes prefetch 후 각 term 마다 빌드
    private fun buildCards(
        terms: List<SimpleTermProjection>,
        bookmarkedBy: (SimpleTermProjection) -> Boolean,
        sourceGlossaryBy: (SimpleTermProjection) -> SourceGlossaryInfo?
    ): List<Card> {
        val termIds = terms.map { it.termId }
        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())
        return terms.map { term ->
            buildCard(
                term = term,
                bookmarked = bookmarkedBy(term),
                sourceGlossary = sourceGlossaryBy(term),
                morphemes = morphemesByTermId[term.termId].orEmpty(),
                variantsByMorphemeId = variantsByMorphemeId
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

    // Phase 2.8 호환용 단일 build (listCards 가 호출). morphemes 인자가 없는 형태
    private fun buildCard(
        term: SimpleTermProjection,
        bookmarked: Boolean,
        sourceGlossary: SourceGlossaryInfo?,
        termIds: List<Long>
    ): Card {
        val morphemesByTermId = fetchMorphemesByTermId(termIds)
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())
        return buildCard(
            term = term,
            bookmarked = bookmarked,
            sourceGlossary = sourceGlossary,
            morphemes = morphemesByTermId[term.termId].orEmpty(),
            variantsByMorphemeId = variantsByMorphemeId
        )
    }

    private fun buildCard(
        term: SimpleTermProjection,
        bookmarked: Boolean,
        sourceGlossary: SourceGlossaryInfo?,
        morphemes: List<BulkOrderedMorphemeProjection>,
        variantsByMorphemeId: Map<Long, List<MorphemeVariantProjection>>
    ): Card = Card(
        termId = term.termId,
        spelling = term.spelling,
        meaning = term.meaning,
        abbreviation = term.abbreviation,
        explanation = term.explanation,
        bookmarked = bookmarked,
        morphemes = buildOrderedMorphemes(term, morphemes, variantsByMorphemeId),
        sourceGlossary = sourceGlossary
    )

    private fun buildCustomNoteCard(
        term: SimpleTermProjection,
        morphemes: List<BulkOrderedMorphemeProjection>,
        variantsByMorphemeId: Map<Long, List<MorphemeVariantProjection>>
    ): CustomNoteCard = CustomNoteCard(
        termId = term.termId,
        spelling = term.spelling,
        meaning = term.meaning,
        abbreviation = term.abbreviation,
        explanation = term.explanation,
        morphemes = buildOrderedMorphemes(term, morphemes, variantsByMorphemeId)
    )

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
