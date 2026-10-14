package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.test.common.dto.TestType
import com.snurdy.backend.domain.test.common.dto.isMultipleChoice
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

data class GlossaryCardSlot(val testTypes: List<TestType>, val isMultipleChoiceGroup: Boolean)

data class GlossarySlotVariant(val outputIndex: Int, val testType: TestType, val count: Int)

@Component
class GlossaryCardCountDistributor {

    fun groupIntoSlots(testTypes: List<TestType>): List<GlossaryCardSlot> {
        if (testTypes.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
        val hasMultipleChoiceGroup = testTypes.filter { it.isMultipleChoice() }.toSet() == MULTIPLE_CHOICE_TYPES
        val slots = mutableListOf<GlossaryCardSlot>()
        var multipleChoiceInserted = false
        for (type in testTypes) {
            if (type.isMultipleChoice() && hasMultipleChoiceGroup) {
                if (!multipleChoiceInserted) {
                    slots += GlossaryCardSlot(MULTIPLE_CHOICE_TYPES.toList(), isMultipleChoiceGroup = true)
                    multipleChoiceInserted = true
                }
            } else {
                slots += GlossaryCardSlot(listOf(type), isMultipleChoiceGroup = false)
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

    fun flattenSlotVariants(slots: List<GlossaryCardSlot>, slotCounts: List<Int>): List<GlossarySlotVariant> {
        val variants = mutableListOf<GlossarySlotVariant>()
        var outputIndex = 0
        for ((slotIndex, slot) in slots.withIndex()) {
            val count = slotCounts[slotIndex]
            if (slot.isMultipleChoiceGroup) {
                val mcCounts = distributeCounts(count, slot.testTypes.size)
                for (i in slot.testTypes.indices) {
                    variants += GlossarySlotVariant(outputIndex++, slot.testTypes[i], mcCounts[i])
                }
            } else {
                variants += GlossarySlotVariant(outputIndex++, slot.testTypes[0], count)
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