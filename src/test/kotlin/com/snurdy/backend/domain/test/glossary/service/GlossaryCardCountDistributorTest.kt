package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.test.common.TestType
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlossaryCardCountDistributorTest {

    private val distributor = GlossaryCardCountDistributor()

    private val multipleChoiceTypes = listOf(
        TestType.MC_MEANING_FROM_TERM,
        TestType.MC_TERM_FROM_ABBREVIATION,
        TestType.MC_TERM_FROM_MEANING
    )

    @Test
    fun `groupIntoSlots 빈 리스트는 TEST_TYPE_NOT_AVAILABLE 예외를 던진다`() {
        val exception = assertThrows(ApiException::class.java) {
            distributor.groupIntoSlots(emptyList())
        }
        assertEquals(TestError.TEST_TYPE_NOT_AVAILABLE, exception.error)
    }

    @Test
    fun `groupIntoSlots 단일 비다지선다 testType 은 1개 슬롯이다`() {
        // When
        val slots = distributor.groupIntoSlots(listOf(TestType.WRITE_MEANING_FROM_TERM))

        // Then
        assertEquals(1, slots.size)
        assertEquals(listOf(TestType.WRITE_MEANING_FROM_TERM), slots[0].testTypes)
        assertFalse(slots[0].isMultipleChoiceGroup)
    }

    @Test
    fun `groupIntoSlots MC 3종이 모두 포함되면 하나의 다지선다 슬롯으로 묶인다`() {
        // When
        val slots = distributor.groupIntoSlots(multipleChoiceTypes)

        // Then
        assertEquals(1, slots.size)
        assertTrue(slots[0].isMultipleChoiceGroup)
        assertEquals(multipleChoiceTypes, slots[0].testTypes)
    }

    @Test
    fun `groupIntoSlots MC 일부만 포함되면 그룹화되지 않고 각자 슬롯이다`() {
        // When
        val slots = distributor.groupIntoSlots(
            listOf(TestType.MC_MEANING_FROM_TERM, TestType.MC_TERM_FROM_MEANING)
        )

        // Then
        assertEquals(2, slots.size)
        assertTrue(slots.none { it.isMultipleChoiceGroup })
    }

    @Test
    fun `groupIntoSlots MC 3종과 비MC 가 혼합되면 MC 는 처음 등장한 위치에 한 슬롯으로 들어간다`() {
        // Given
        val testTypes = listOf(
            TestType.WRITE_TERM_FROM_MEANING,
            TestType.MC_MEANING_FROM_TERM,
            TestType.WRITE_MEANING_FROM_TERM,
            TestType.MC_TERM_FROM_ABBREVIATION,
            TestType.MC_TERM_FROM_MEANING
        )

        // When
        val slots = distributor.groupIntoSlots(testTypes)

        // Then
        assertEquals(3, slots.size)
        assertEquals(listOf(TestType.WRITE_TERM_FROM_MEANING), slots[0].testTypes)
        assertTrue(slots[1].isMultipleChoiceGroup)
        assertEquals(listOf(TestType.WRITE_MEANING_FROM_TERM), slots[2].testTypes)
    }

    @Test
    fun `distributeCounts 나누어 떨어지면 모두 같은 값으로 분배된다`() {
        assertEquals(listOf(5, 5), distributor.distributeCounts(total = 10, parts = 2))
    }

    @Test
    fun `distributeCounts 나머지는 앞쪽 슬롯부터 1씩 더 받는다`() {
        assertEquals(listOf(4, 3, 3), distributor.distributeCounts(total = 10, parts = 3))
    }

    @Test
    fun `distributeCounts parts 가 0 이하면 빈 리스트를 반환한다`() {
        assertEquals(emptyList<Int>(), distributor.distributeCounts(total = 10, parts = 0))
    }

    @Test
    fun `flattenSlotVariants MC 그룹 슬롯은 3 variants 로 펼쳐지고 outputIndex 가 순서대로 매겨진다`() {
        // Given
        val slots = listOf(
            GlossaryCardSlot(listOf(TestType.WRITE_MEANING_FROM_TERM), isMultipleChoiceGroup = false),
            GlossaryCardSlot(multipleChoiceTypes, isMultipleChoiceGroup = true)
        )

        // When
        val variants = distributor.flattenSlotVariants(slots, slotCounts = listOf(5, 10))

        // Then
        assertEquals(
            listOf(
                GlossarySlotVariant(outputIndex = 0, testType = TestType.WRITE_MEANING_FROM_TERM, count = 5),
                GlossarySlotVariant(outputIndex = 1, testType = TestType.MC_MEANING_FROM_TERM, count = 4),
                GlossarySlotVariant(outputIndex = 2, testType = TestType.MC_TERM_FROM_ABBREVIATION, count = 3),
                GlossarySlotVariant(outputIndex = 3, testType = TestType.MC_TERM_FROM_MEANING, count = 3)
            ),
            variants
        )
    }
}