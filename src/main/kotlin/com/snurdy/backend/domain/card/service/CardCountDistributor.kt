package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.test.entity.isMultipleChoice
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

// 슬롯 그룹화 + 슬롯/변형별 count 분배 순수 계산
// MC 3종이 testTypes 에 모두 있으면 한 다지선다 슬롯으로 묶고, 그 외 testType 은 각자 1 슬롯
data class CardSlot(val testTypes: List<TestType>, val isMultipleChoiceGroup: Boolean)

// 슬롯을 다지선다 변형까지 펼친 출제 단위. outputIndex 는 카드 응답 순서 복원용
data class SlotVariant(val outputIndex: Int, val testType: TestType, val count: Int)

@Component
class CardCountDistributor {

    fun groupIntoSlots(testTypes: List<TestType>): List<CardSlot> {
        if (testTypes.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
        val hasMultipleChoiceGroup = testTypes.filter { it.isMultipleChoice() }.toSet() == MULTIPLE_CHOICE_TYPES
        val slots = mutableListOf<CardSlot>()
        var multipleChoiceInserted = false
        for (type in testTypes) {
            if (type.isMultipleChoice() && hasMultipleChoiceGroup) {
                if (!multipleChoiceInserted) {
                    slots += CardSlot(MULTIPLE_CHOICE_TYPES.toList(), isMultipleChoiceGroup = true)
                    multipleChoiceInserted = true
                }
            } else {
                slots += CardSlot(listOf(type), isMultipleChoiceGroup = false)
            }
        }
        return slots
    }

    fun distributeCounts(total: Int, parts: Int): List<Int> {
        if (parts <= 0) return emptyList()
        val base = total / parts
        val remainder = total % parts
        return List(parts) { index -> base + if (index < remainder) 1 else 0 }
    }

    fun flattenSlotVariants(slots: List<CardSlot>, slotCounts: List<Int>): List<SlotVariant> {
        val variants = mutableListOf<SlotVariant>()
        var outputIndex = 0
        for ((slotIndex, slot) in slots.withIndex()) {
            val count = slotCounts[slotIndex]
            if (slot.isMultipleChoiceGroup) {
                val mcCounts = distributeCounts(count, slot.testTypes.size)
                for (i in slot.testTypes.indices) {
                    variants += SlotVariant(outputIndex++, slot.testTypes[i], mcCounts[i])
                }
            } else {
                variants += SlotVariant(outputIndex++, slot.testTypes[0], count)
            }
        }
        return variants
    }

    companion object {
        private val MULTIPLE_CHOICE_TYPES = setOf(
            TestType.MC_MEANING_FROM_TERM,
            TestType.MC_TERM_FROM_ABBREVIATION,
            TestType.MC_TERM_FROM_MEANING
        )
    }
}
