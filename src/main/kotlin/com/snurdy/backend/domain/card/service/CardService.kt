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
import com.snurdy.backend.domain.card.dto.toStudyTerm
import com.snurdy.backend.domain.card.dto.toWrongAnswerCard
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.service.MorphemeHighlightAssembler
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.test.entity.deriveAnswer
import com.snurdy.backend.domain.test.entity.deriveQuestion
import com.snurdy.backend.domain.test.entity.isMultipleChoice
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
    private val cardTermDistributor: CardTermDistributor,
    private val distractorSelector: DistractorSelector,
    private val morphemeHighlightAssembler: MorphemeHighlightAssembler,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun listGlossaryCards(userId: UUID, glossaryId: Long, request: GlossaryCardsRequest): GlossaryCardsResponse {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        // 학습 세트를 테스트 유형과 무관하게 먼저 뽑고 세트 안에서 유형별 출제 가능 단어만 배정
        val sampledTerms = glossaryTermRepository.pickCardTermsForTest(
            userId = userId,
            glossaryId = glossaryId,
            count = request.count
        )
        val slots = cardCountDistributor.groupIntoSlots(request.testTypes)
        val assignments = cardTermDistributor.distribute(sampledTerms, slots)
        if (assignments.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val distractorPool = if (assignments.any { it.second.isMultipleChoice() }) {
            glossaryTermRepository.findAllSimpleTermsByGlossaryId(glossaryId)
        } else {
            emptyList()
        }

        // 학습 화면용 terms 에도 북마크와 형광펜이 필요하므로 학습 세트 전체 기준으로 조회
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

        val cards = assignments.map { (term, testType) ->
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
        return GlossaryCardsResponse(
            requestedCount = request.count,
            availableCount = cards.size,
            terms = terms,
            cards = cards
        )
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
        val assignments = cardTermDistributor.distribute(terms, slots)
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
        val assignments = cardTermDistributor.distribute(terms, slots)
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
}
