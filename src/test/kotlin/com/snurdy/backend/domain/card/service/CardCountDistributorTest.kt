package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardCountDistributorTest {

    private val distributor = CardCountDistributor()

    @Test
    fun `groupIntoSlots 빈 리스트는 TEST_TYPE_NOT_AVAILABLE 예외를 던진다`() {
        val exception = assertThrows(ApiException::class.java) {
            distributor.groupIntoSlots(emptyList())
        }
        assertEquals(TestError.TEST_TYPE_NOT_AVAILABLE, exception.error)
    }

    @Test
    fun `groupIntoSlots 단일 비다지선다 testType 은 1개 슬롯이다`() {
        // Given
        val testTypes = listOf(TestType.WRITE_TERM_FROM_MEANING)

        // When
        val slots = distributor.groupIntoSlots(testTypes)

        // Then
        assertEquals(1, slots.size)
        assertEquals(listOf(TestType.WRITE_TERM_FROM_MEANING), slots[0].testTypes)
        assertFalse(slots[0].isMultipleChoiceGroup)
    }

    @Test
    fun `groupIntoSlots MC 3종이 모두 포함되면 하나의 다지선다 슬롯으로 묶인다`() {
        // Given
        val testTypes = listOf(
            TestType.MC_MEANING_FROM_TERM,
            TestType.MC_TERM_FROM_ABBREVIATION,
            TestType.MC_TERM_FROM_MEANING
        )

        // When
        val slots = distributor.groupIntoSlots(testTypes)

        // Then
        assertEquals(1, slots.size)
        assertTrue(slots[0].isMultipleChoiceGroup)
        assertEquals(3, slots[0].testTypes.size)
    }

    @Test
    fun `groupIntoSlots MC 일부만 포함되면 그룹화되지 않고 각자 슬롯이다`() {
        // Given
        val testTypes = listOf(TestType.MC_MEANING_FROM_TERM, TestType.MC_TERM_FROM_MEANING)

        // When
        val slots = distributor.groupIntoSlots(testTypes)

        // Then
        assertEquals(2, slots.size)
        assertFalse(slots[0].isMultipleChoiceGroup)
        assertFalse(slots[1].isMultipleChoiceGroup)
    }

    @Test
    fun `groupIntoSlots MC 3종과 비MC 가 혼합되면 MC 1슬롯 비MC 슬롯들이 입력 순서대로 구성된다`() {
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
        // 비MC 1개 + MC group 1개 (MC 등장 위치에 한 번만 삽입) + 비MC 1개
        assertEquals(3, slots.size)
        assertEquals(listOf(TestType.WRITE_TERM_FROM_MEANING), slots[0].testTypes)
        assertFalse(slots[0].isMultipleChoiceGroup)
        assertTrue(slots[1].isMultipleChoiceGroup)
        assertEquals(listOf(TestType.WRITE_MEANING_FROM_TERM), slots[2].testTypes)
    }

    @Test
    fun `distributeCounts 나누어 떨어지면 모두 같은 값으로 분배된다`() {
        val counts = distributor.distributeCounts(total = 10, parts = 5)
        assertEquals(listOf(2, 2, 2, 2, 2), counts)
    }

    @Test
    fun `distributeCounts 나머지는 앞쪽 슬롯부터 1씩 더 받는다`() {
        val counts = distributor.distributeCounts(total = 10, parts = 3)
        assertEquals(listOf(4, 3, 3), counts)
    }

    @Test
    fun `distributeCounts total 이 0 이어도 parts 만큼 0 으로 채운다`() {
        val counts = distributor.distributeCounts(total = 0, parts = 3)
        assertEquals(listOf(0, 0, 0), counts)
    }

    @Test
    fun `distributeCounts parts 가 0 이하면 빈 리스트를 반환한다`() {
        assertEquals(emptyList<Int>(), distributor.distributeCounts(total = 10, parts = 0))
        assertEquals(emptyList<Int>(), distributor.distributeCounts(total = 10, parts = -1))
    }

    @Test
    fun `flattenSlotVariants 비MC 슬롯은 그대로 1 variant 로 펼쳐진다`() {
        // Given
        val slots = listOf(CardSlot(listOf(TestType.WRITE_TERM_FROM_MEANING), isMultipleChoiceGroup = false))
        val slotCounts = listOf(5)

        // When
        val variants = distributor.flattenSlotVariants(slots, slotCounts)

        // Then
        assertEquals(1, variants.size)
        assertEquals(SlotVariant(outputIndex = 0, testType = TestType.WRITE_TERM_FROM_MEANING, count = 5), variants[0])
    }

    @Test
    fun `flattenSlotVariants MC 그룹 슬롯은 3 variants 로 펼쳐지고 count 가 균등 분배된다`() {
        // Given
        val mcSlot = CardSlot(
            testTypes = listOf(
                TestType.MC_MEANING_FROM_TERM,
                TestType.MC_TERM_FROM_ABBREVIATION,
                TestType.MC_TERM_FROM_MEANING
            ),
            isMultipleChoiceGroup = true
        )
        val slotCounts = listOf(7)

        // When
        val variants = distributor.flattenSlotVariants(listOf(mcSlot), slotCounts)

        // Then
        assertEquals(3, variants.size)
        assertEquals(listOf(3, 2, 2), variants.map { it.count })
        assertEquals(listOf(0, 1, 2), variants.map { it.outputIndex })
    }

    @Test
    fun `flattenSlotVariants 혼합 슬롯은 outputIndex 가 입력 순서대로 연속 부여된다`() {
        // Given
        val slots = listOf(
            CardSlot(listOf(TestType.WRITE_TERM_FROM_MEANING), isMultipleChoiceGroup = false),
            CardSlot(
                testTypes = listOf(
                    TestType.MC_MEANING_FROM_TERM,
                    TestType.MC_TERM_FROM_ABBREVIATION,
                    TestType.MC_TERM_FROM_MEANING
                ),
                isMultipleChoiceGroup = true
            ),
            CardSlot(listOf(TestType.LISTEN_AND_WRITE), isMultipleChoiceGroup = false)
        )
        val slotCounts = listOf(4, 6, 5)

        // When
        val variants = distributor.flattenSlotVariants(slots, slotCounts)

        // Then
        assertEquals(5, variants.size)
        assertEquals(listOf(0, 1, 2, 3, 4), variants.map { it.outputIndex })
        assertEquals(TestType.WRITE_TERM_FROM_MEANING, variants[0].testType)
        assertEquals(TestType.LISTEN_AND_WRITE, variants[4].testType)
        assertEquals(4, variants[0].count)
        // MC 6개 균등 분배
        assertEquals(listOf(2, 2, 2), variants.subList(1, 4).map { it.count })
        assertEquals(5, variants[4].count)
    }

}
