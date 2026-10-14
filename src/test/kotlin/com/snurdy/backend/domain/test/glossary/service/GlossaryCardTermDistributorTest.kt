package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.test.common.dto.TestType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlossaryCardTermDistributorTest {

    private val countDistributor = GlossaryCardCountDistributor()
    private val distributor = GlossaryCardTermDistributor(countDistributor)

    private data class FakeTerm(
        override val termId: Long,
        override val spelling: String,
        override val meaning: String,
        override val abbreviation: String?,
        override val explanation: String?
    ) : SimpleTermProjection

    private fun term(termId: Long, abbreviation: String? = null): FakeTerm = FakeTerm(
        termId = termId,
        spelling = "spelling$termId",
        meaning = "meaning$termId",
        abbreviation = abbreviation,
        explanation = null
    )

    // 앞에서 abbreviationCount 개만 약어 보유
    private fun terms(total: Int, abbreviationCount: Int): List<FakeTerm> =
        (1L..total.toLong()).map { term(it, abbreviation = if (it <= abbreviationCount) "A$it" else null) }

    private fun distribute(terms: List<FakeTerm>, testTypes: List<TestType>): List<Pair<FakeTerm, TestType>> =
        distributor.distribute(terms, countDistributor.groupIntoSlots(testTypes))

    private fun List<Pair<FakeTerm, TestType>>.countsByType(): Map<TestType, Int> =
        groupingBy { it.second }.eachCount()

    @Test
    fun `단일 유형을 선택하면 학습 단어 수만큼 출제된다`() {
        // Given
        val studied = terms(total = 10, abbreviationCount = 3)

        // When
        val assignments = distribute(studied, listOf(TestType.WRITE_MEANING_FROM_TERM))

        // Then
        assertEquals(10, assignments.size)
        assertEquals(studied.map { it.termId }.toSet(), assignments.map { it.first.termId }.toSet())
    }

    @Test
    fun `학습 단어는 한 번씩만 출제된다`() {
        // Given
        val studied = terms(total = 10, abbreviationCount = 5)

        // When
        val assignments = distribute(
            studied,
            listOf(TestType.WRITE_MEANING_FROM_TERM, TestType.WRITE_ABBREVIATION_FROM_TERM)
        )

        // Then
        val termIds = assignments.map { it.first.termId }
        assertEquals(termIds.size, termIds.toSet().size)
    }

    @Test
    fun `약어 쓰기와 다른 유형을 함께 선택하면 약어 부족분이 다른 유형으로 넘어가 합계가 유지된다`() {
        // Given 10개 중 약어 보유 3개 슬롯별 배정량은 5씩
        val studied = terms(total = 10, abbreviationCount = 3)

        // When
        val assignments = distribute(
            studied,
            listOf(TestType.WRITE_MEANING_FROM_TERM, TestType.WRITE_ABBREVIATION_FROM_TERM)
        )

        // Then
        val countsByType = assignments.countsByType()
        assertEquals(10, assignments.size)
        assertEquals(3, countsByType[TestType.WRITE_ABBREVIATION_FROM_TERM])
        assertEquals(7, countsByType[TestType.WRITE_MEANING_FROM_TERM])
    }

    @Test
    fun `약어 보유 단어가 없으면 약어 유형은 0장이고 나머지 유형이 전부 출제된다`() {
        // Given
        val studied = terms(total = 10, abbreviationCount = 0)

        // When
        val assignments = distribute(
            studied,
            listOf(TestType.WRITE_MEANING_FROM_TERM, TestType.WRITE_ABBREVIATION_FROM_TERM)
        )

        // Then
        val countsByType = assignments.countsByType()
        assertEquals(10, countsByType[TestType.WRITE_MEANING_FROM_TERM])
        assertEquals(null, countsByType[TestType.WRITE_ABBREVIATION_FROM_TERM])
    }

    @Test
    fun `약어 쓰기만 선택하면 약어 보유 단어 수만큼만 출제된다`() {
        // Given
        val studied = terms(total = 10, abbreviationCount = 4)

        // When
        val assignments = distribute(studied, listOf(TestType.WRITE_ABBREVIATION_FROM_TERM))

        // Then
        assertEquals(4, assignments.size)
        assertTrue(assignments.all { !it.first.abbreviation.isNullOrBlank() })
    }

    @Test
    fun `약어 쓰기만 선택했는데 약어 보유 단어가 없으면 빈 결과다`() {
        // Given
        val studied = terms(total = 10, abbreviationCount = 0)

        // When
        val assignments = distribute(studied, listOf(TestType.WRITE_ABBREVIATION_FROM_TERM))

        // Then
        assertTrue(assignments.isEmpty())
    }

    @Test
    fun `다지선다 3종을 선택하면 약어 다지선다 부족분이 다른 다지선다로 넘어가 합계가 유지된다`() {
        // Given 10개 중 약어 보유 1개 MC 배정량은 4 3 3
        val studied = terms(total = 10, abbreviationCount = 1)

        // When
        val assignments = distribute(
            studied,
            listOf(
                TestType.MC_MEANING_FROM_TERM,
                TestType.MC_TERM_FROM_ABBREVIATION,
                TestType.MC_TERM_FROM_MEANING
            )
        )

        // Then
        val countsByType = assignments.countsByType()
        assertEquals(10, assignments.size)
        assertEquals(1, countsByType[TestType.MC_TERM_FROM_ABBREVIATION])
        assertEquals(9, countsByType.getValue(TestType.MC_MEANING_FROM_TERM) + countsByType.getValue(TestType.MC_TERM_FROM_MEANING))
    }

    @Test
    fun `약어 유형에는 약어 보유 단어만 배정된다`() {
        // Given
        val studied = terms(total = 10, abbreviationCount = 6)

        // When
        val assignments = distribute(
            studied,
            listOf(
                TestType.WRITE_MEANING_FROM_TERM,
                TestType.MC_MEANING_FROM_TERM,
                TestType.MC_TERM_FROM_ABBREVIATION,
                TestType.MC_TERM_FROM_MEANING
            )
        )

        // Then
        assertEquals(10, assignments.size)
        assertTrue(
            assignments
                .filter { it.second == TestType.MC_TERM_FROM_ABBREVIATION }
                .all { !it.first.abbreviation.isNullOrBlank() }
        )
    }
}