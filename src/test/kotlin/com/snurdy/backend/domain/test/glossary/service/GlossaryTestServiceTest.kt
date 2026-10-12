package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.dictionary.morpheme.service.MorphemeHighlightAssembler
import com.snurdy.backend.domain.test.common.dto.TestType
import com.snurdy.backend.domain.test.common.service.TestSubmissionService
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsRequest
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID

class GlossaryTestServiceTest {

    private lateinit var glossaryRepository: GlossaryRepository
    private lateinit var glossaryTermRepository: GlossaryTermRepository
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var distractorSelector: GlossaryDistractorSelector
    private lateinit var morphemeHighlightAssembler: MorphemeHighlightAssembler
    private lateinit var glossaryTestService: GlossaryTestService
    private lateinit var testSubmissionService: TestSubmissionService

    private val userId = UUID.randomUUID()
    private val glossaryId = 12L

    private data class FakeTerm(
        override val termId: Long,
        override val spelling: String,
        override val meaning: String,
        override val abbreviation: String?,
        override val explanation: String?
    ) : SimpleTermProjection

    private fun term(termId: Long): FakeTerm = FakeTerm(
        termId = termId,
        spelling = "spelling$termId",
        meaning = "meaning$termId, synonym$termId",
        abbreviation = null,
        explanation = null
    )

    @BeforeEach
    fun setUp() {
        glossaryRepository = mockk()
        glossaryTermRepository = mockk()
        bookmarkRepository = mockk()
        distractorSelector = mockk()
        morphemeHighlightAssembler = mockk()
        val countDistributor = GlossaryCardCountDistributor()
        glossaryTestService = GlossaryTestService(
            glossaryRepository = glossaryRepository,
            glossaryTermRepository = glossaryTermRepository,
            bookmarkRepository = bookmarkRepository,
            cardCountDistributor = countDistributor,
            cardTermDistributor = GlossaryCardTermDistributor(countDistributor),
            distractorSelector = distractorSelector,
            morphemeHighlightAssembler = morphemeHighlightAssembler,
            glossaryProgressRepository = mockk(),
            dailyProgressRepository = mockk(),
            progressUpdateService = mockk(),
            clock = Clock.systemUTC(),
            testSubmissionService = mockk()
        )

        every { glossaryRepository.existsById(glossaryId) } returns true
        every { bookmarkRepository.findBookmarkedTermIds(userId, glossaryId, any()) } returns emptyList()
        every { morphemeHighlightAssembler.buildByTermIds(any(), any()) } returns emptyMap()
    }

    @Test
    fun `요청한 termIds 의 단어로만 카드가 생성된다`() {
        // Given
        val requestedTermIds = (1L..10L).toList()
        every {
            glossaryTermRepository.findSimpleTermsByGlossaryIdAndTermIds(glossaryId, requestedTermIds)
        } returns requestedTermIds.map { term(it) }

        // When
        val response = glossaryTestService.listTestCards(
            userId,
            glossaryId,
            GlossaryTestCardsRequest(termIds = requestedTermIds, testTypes = listOf(TestType.WRITE_MEANING_FROM_TERM))
        )

        // Then
        assertEquals(10, response.cards.size)
        assertEquals(requestedTermIds.toSet(), response.cards.map { it.termId }.toSet())
        assertEquals(mapOf(TestType.WRITE_MEANING_FROM_TERM to 10), response.availableCounts)
        verify(exactly = 0) { glossaryTermRepository.findAllSimpleTermsByGlossaryId(any()) }
    }

    @Test
    fun `작성형 카드는 콤마 동의어를 answers 로 분리하고 distractors 는 null 이다`() {
        // Given
        val requestedTermIds = listOf(1L)
        every {
            glossaryTermRepository.findSimpleTermsByGlossaryIdAndTermIds(glossaryId, requestedTermIds)
        } returns listOf(term(1L))

        // When
        val card = glossaryTestService.listTestCards(
            userId,
            glossaryId,
            GlossaryTestCardsRequest(termIds = requestedTermIds, testTypes = listOf(TestType.WRITE_MEANING_FROM_TERM))
        ).cards.single()

        // Then
        assertEquals("meaning1, synonym1", card.answer)
        assertEquals(listOf("meaning1", "synonym1"), card.answers)
        assertEquals(null, card.distractors)
    }

    @Test
    fun `여러 유형을 선택하면 카드 순서는 섞이고 유형별 개수는 유지된다`() {
        // Given
        val requestedTermIds = (1L..40L).toList()
        every {
            glossaryTermRepository.findSimpleTermsByGlossaryIdAndTermIds(glossaryId, requestedTermIds)
        } returns requestedTermIds.map { term(it) }
        val request = GlossaryTestCardsRequest(
            termIds = requestedTermIds,
            testTypes = listOf(TestType.WRITE_MEANING_FROM_TERM, TestType.WRITE_TERM_FROM_MEANING)
        )
        val groupedOrder = List(20) { TestType.WRITE_MEANING_FROM_TERM } + List(20) { TestType.WRITE_TERM_FROM_MEANING }

        // When 셔플 결과가 우연히 묶인 순서와 같을 확률을 없애기 위해 여러 번 호출
        val responses = List(5) { glossaryTestService.listTestCards(userId, glossaryId, request) }

        // Then
        responses.forEach {
            assertEquals(
                mapOf(TestType.WRITE_MEANING_FROM_TERM to 20, TestType.WRITE_TERM_FROM_MEANING to 20),
                it.availableCounts
            )
        }
        assertTrue(responses.any { response -> response.cards.map { it.testType } != groupedOrder })
    }

    @Test
    fun `단어장에 속한 단어가 하나도 없으면 TEST_TYPE_NOT_AVAILABLE 예외를 던진다`() {
        // Given
        val requestedTermIds = listOf(999L)
        every {
            glossaryTermRepository.findSimpleTermsByGlossaryIdAndTermIds(glossaryId, requestedTermIds)
        } returns emptyList()

        // When
        val exception = assertThrows(ApiException::class.java) {
            glossaryTestService.listTestCards(
                userId,
                glossaryId,
                GlossaryTestCardsRequest(termIds = requestedTermIds, testTypes = listOf(TestType.WRITE_MEANING_FROM_TERM))
            )
        }

        // Then
        assertEquals(TestError.TEST_TYPE_NOT_AVAILABLE, exception.error)
    }

    @Test
    fun `존재하지 않는 단어장이면 GLOSSARY_NOT_FOUND 예외를 던진다`() {
        // Given
        every { glossaryRepository.existsById(glossaryId) } returns false

        // When
        val exception = assertThrows(ApiException::class.java) {
            glossaryTestService.listTestCards(
                userId,
                glossaryId,
                GlossaryTestCardsRequest(termIds = listOf(1L), testTypes = listOf(TestType.WRITE_MEANING_FROM_TERM))
            )
        }

        // Then
        assertEquals(DictionaryError.GLOSSARY_NOT_FOUND, exception.error)
    }
}