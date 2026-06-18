package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.card.dto.CustomNoteCardsResponse
import com.snurdy.backend.domain.card.dto.GlossaryCardsRequest
import com.snurdy.backend.domain.card.dto.GlossaryCardsResponse
import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.card.dto.WrongAnswerCard
import com.snurdy.backend.domain.card.dto.WrongAnswerCardsResponse
import com.snurdy.backend.domain.card.dto.toCustomNoteCard
import com.snurdy.backend.domain.card.dto.toGlossaryCard
import com.snurdy.backend.domain.card.dto.toWrongAnswerCard
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.dictionary.morpheme.service.MorphemeHighlightAssembler
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.test.entity.deriveAnswer
import com.snurdy.backend.domain.test.entity.deriveQuestion
import com.snurdy.backend.domain.test.entity.isMultipleChoice
import com.snurdy.backend.domain.test.entity.requiresAbbreviation
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
    private val customNoteService: CustomNoteService,
    private val customNoteTermRepository: CustomNoteTermRepository,
    private val wrongAnswerTermRepository: WrongAnswerTermRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val cardCountDistributor: CardCountDistributor,
    private val noteTermDistributor: NoteTermDistributor,
    private val distractorSelector: DistractorSelector,
    private val morphemeHighlightAssembler: MorphemeHighlightAssembler,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun listGlossaryCards(userId: UUID, glossaryId: Long, request: GlossaryCardsRequest): GlossaryCardsResponse {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val slots = cardCountDistributor.groupIntoSlots(request.testTypes)
        val slotCounts = cardCountDistributor.distributeCounts(request.count, slots.size)
        val variants = cardCountDistributor.flattenSlotVariants(slots, slotCounts)
        val perTypeTotals = cardCountDistributor.aggregateCountsByType(variants)

        val pickedByType = perTypeTotals.mapValues { (testType, total) ->
            if (total == 0) emptyList() else glossaryTermRepository.pickCardTermsForTest(
                userId = userId,
                glossaryId = glossaryId,
                requireAbbreviation = testType.requiresAbbreviation(),
                count = total
            ).also { picked ->
                if (picked.size < total) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
            }
        }

        val cardOrder = orderGlossaryTermsAcrossVariants(variants, pickedByType)
        if (cardOrder.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val distractorPool = if (variants.any { it.testType.isMultipleChoice() && it.count > 0 }) {
            glossaryTermRepository.findAllSimpleTermsByGlossaryId(glossaryId)
        } else {
            emptyList()
        }

        val termIds = cardOrder.map { it.first.termId }.distinct()
        val bookmarkedTermIds = bookmarkRepository
            .findBookmarkedTermIds(userId, glossaryId, termIds)
            .toSet()
        val morphemesByTermId = morphemeHighlightAssembler.buildByTermIds(
            termIds = termIds,
            spellingByTermId = cardOrder.associate { it.first.termId to it.first.spelling }
        )

        val cards = cardOrder.map { (term, testType) ->
            val answer = testType.deriveAnswer(term.spelling, term.meaning, term.abbreviation)
            term.toGlossaryCard(
                testType = testType,
                question = testType.deriveQuestion(term.spelling, term.meaning, term.abbreviation),
                answer = answer,
                distractors = if (testType.isMultipleChoice()) {
                    distractorSelector.select(term.termId, answer, testType, distractorPool)
                } else null,
                bookmarked = term.termId in bookmarkedTermIds,
                morphemes = morphemesByTermId[term.termId].orEmpty()
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
        val terms = customNoteTermRepository.findCardTermsByNoteId(noteId = customNoteId, requireAbbreviation = false)
        if (terms.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val slots = cardCountDistributor.groupIntoSlots(request.testTypes)
        val assignments = noteTermDistributor.distribute(terms, slots)
        if (assignments.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val context = buildNoteCardContext(userId, assignments)

        val cards = assignments.map { (term, testType) ->
            val answer = testType.deriveAnswer(term.spelling, term.meaning, term.abbreviation)
            term.toCustomNoteCard(
                testType = testType,
                question = testType.deriveQuestion(term.spelling, term.meaning, term.abbreviation),
                answer = answer,
                distractors = if (testType.isMultipleChoice()) {
                    distractorSelector.select(term.termId, answer, testType, terms)
                } else null,
                bookmarked = (term.glossaryId to term.termId) in context.bookmarkedPairs,
                morphemes = context.morphemesByTermId[term.termId].orEmpty()
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
        val terms = wrongAnswerTermRepository.findCardTermsByPeriod(
            userId = userId,
            after = after,
            requireAbbreviation = false
        )
        if (terms.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        return WrongAnswerCardsResponse(cards = buildWrongAnswerCards(userId, terms, request.testTypes))
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
        val terms = wrongAnswerTermRepository.findCardTermsByGlossary(
            userId = userId,
            glossaryId = glossaryId,
            requireAbbreviation = false
        )
        if (terms.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        return WrongAnswerCardsResponse(cards = buildWrongAnswerCards(userId, terms, request.testTypes))
    }

    // 오답노트 카드 빌드 — by-period / by-glossary 공용
    private fun buildWrongAnswerCards(
        userId: UUID,
        terms: List<NoteCardTermProjection>,
        testTypes: List<TestType>
    ): List<WrongAnswerCard> {
        val slots = cardCountDistributor.groupIntoSlots(testTypes)
        val assignments = noteTermDistributor.distribute(terms, slots)
        if (assignments.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val context = buildNoteCardContext(userId, assignments)

        return assignments.map { (term, testType) ->
            val answer = testType.deriveAnswer(term.spelling, term.meaning, term.abbreviation)
            term.toWrongAnswerCard(
                testType = testType,
                question = testType.deriveQuestion(term.spelling, term.meaning, term.abbreviation),
                answer = answer,
                distractors = if (testType.isMultipleChoice()) {
                    distractorSelector.select(term.termId, answer, testType, terms)
                } else null,
                bookmarked = (term.glossaryId to term.termId) in context.bookmarkedPairs,
                morphemes = context.morphemesByTermId[term.termId].orEmpty()
            )
        }
    }

    private data class NoteCardContext(
        val bookmarkedPairs: Set<Pair<Long, Long>>,
        val morphemesByTermId: Map<Long, List<com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme>>
    )

    private fun buildNoteCardContext(
        userId: UUID,
        assignments: List<Pair<NoteCardTermProjection, TestType>>
    ): NoteCardContext {
        val termIds = assignments.map { it.first.termId }.distinct()
        val glossaryIds = assignments.mapNotNull { it.first.glossaryId.takeIf { id -> id > 0 } }.toSet()
        val bookmarkedPairs = if (glossaryIds.isEmpty()) emptySet() else bookmarkRepository
            .findBookmarkedPairs(userId, glossaryIds, termIds)
            .mapTo(mutableSetOf()) { it.glossaryId to it.termId }
        val morphemesByTermId = morphemeHighlightAssembler.buildByTermIds(
            termIds = termIds,
            spellingByTermId = assignments.associate { it.first.termId to it.first.spelling }
        )
        return NoteCardContext(bookmarkedPairs, morphemesByTermId)
    }

    // 기본 단어장 sampling 결과를 입력 변형 순서로 cards 에 배치
    private fun orderGlossaryTermsAcrossVariants(
        variants: List<SlotVariant>,
        pickedByType: Map<TestType, List<SimpleTermProjection>>
    ): List<Pair<SimpleTermProjection, TestType>> {
        val cursors = mutableMapOf<TestType, Int>()
        val result = mutableListOf<Pair<SimpleTermProjection, TestType>>()
        for (variant in variants) {
            if (variant.count == 0) continue
            val source = pickedByType[variant.testType] ?: continue
            val from = cursors.getOrDefault(variant.testType, 0)
            for (index in from until from + variant.count) {
                result += source[index] to variant.testType
            }
            cursors[variant.testType] = from + variant.count
        }
        return result
    }
}
