package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.test.legacy.entity.TestType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardTermDistributorTest {

    private val distributor = CardTermDistributor(CardCountDistributor())

    private data class FakeCardTerm(
        override val termId: Long,
        override val spelling: String,
        override val meaning: String,
        override val abbreviation: String?,
        override val explanation: String?
    ) : SimpleTermProjection

    private fun term(termId: Long, abbreviation: String? = null): FakeCardTerm = FakeCardTerm(
        termId = termId,
        spelling = "spelling$termId",
        meaning = "meaning$termId",
        abbreviation = abbreviation,
        explanation = null
    )

    private fun slot(testType: TestType): CardSlot = CardSlot(listOf(testType), isMultipleChoiceGroup = false)

    @Test
    fun `약어 변형만 요청하면 약어 보유 단어 수만큼만 배정된다`() {
        // Given 10개 중 약어 보유 5개
        val terms = (1L..10L).map { term(it, abbreviation = if (it <= 5) "A$it" else null) }
        val slots = listOf(slot(TestType.WRITE_ABBREVIATION_FROM_TERM))

        // When
        val assignments = distributor.distribute(terms, slots)

        // Then
        assertEquals(5, assignments.size)
        assertTrue(assignments.all { !it.first.abbreviation.isNullOrBlank() })
        assertTrue(assignments.all { it.second == TestType.WRITE_ABBREVIATION_FROM_TERM })
    }

    @Test
    fun `혼합 변형에서 약어 변형만 부족분만큼 줄어든다`() {
        // Given 10개 중 약어 보유 4개, 슬롯별 배정량은 5씩
        val terms = (1L..10L).map { term(it, abbreviation = if (it <= 4) "A$it" else null) }
        val slots = listOf(
            slot(TestType.WRITE_TERM_FROM_MEANING),
            slot(TestType.WRITE_ABBREVIATION_FROM_TERM)
        )

        // When
        val assignments = distributor.distribute(terms, slots)

        // Then
        val countsByType = assignments.groupingBy { it.second }.eachCount()
        assertEquals(5, countsByType[TestType.WRITE_TERM_FROM_MEANING])
        assertEquals(4, countsByType[TestType.WRITE_ABBREVIATION_FROM_TERM])
    }

    @Test
    fun `다지선다 그룹에서 약어 요구 변형만 줄고 나머지 변형은 유지된다`() {
        // Given 9개 중 약어 보유 2개, MC 3변형 배정량은 3씩
        val terms = (1L..9L).map { term(it, abbreviation = if (it <= 2) "A$it" else null) }
        val slots = listOf(
            CardSlot(
                listOf(
                    TestType.MC_MEANING_FROM_TERM,
                    TestType.MC_TERM_FROM_ABBREVIATION,
                    TestType.MC_TERM_FROM_MEANING
                ),
                isMultipleChoiceGroup = true
            )
        )

        // When
        val assignments = distributor.distribute(terms, slots)

        // Then
        val countsByType = assignments.groupingBy { it.second }.eachCount()
        assertEquals(3, countsByType[TestType.MC_MEANING_FROM_TERM])
        assertEquals(2, countsByType[TestType.MC_TERM_FROM_ABBREVIATION])
        assertEquals(3, countsByType[TestType.MC_TERM_FROM_MEANING])
    }

    @Test
    fun `배정 결과는 입력 슬롯 순서를 유지한다`() {
        // Given 약어 변형이 뒤 슬롯이어도 응답 순서는 슬롯 순서
        val terms = (1L..6L).map { term(it, abbreviation = "A$it") }
        val slots = listOf(
            slot(TestType.WRITE_TERM_FROM_MEANING),
            slot(TestType.WRITE_ABBREVIATION_FROM_TERM)
        )

        // When
        val assignments = distributor.distribute(terms, slots)

        // Then
        assertEquals(6, assignments.size)
        assertEquals(
            List(3) { TestType.WRITE_TERM_FROM_MEANING } + List(3) { TestType.WRITE_ABBREVIATION_FROM_TERM },
            assignments.map { it.second }
        )
    }
}
