package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.test.common.TestType
import com.snurdy.backend.domain.test.common.requiresAbbreviation
import org.springframework.stereotype.Component

@Component
class GlossaryCardTermDistributor(
    private val cardCountDistributor: GlossaryCardCountDistributor
) {

    fun <T : SimpleTermProjection> distribute(
        terms: List<T>,
        slots: List<GlossaryCardSlot>
    ): List<Pair<T, TestType>> {
        if (slots.isEmpty() || terms.isEmpty()) return emptyList()
        val shuffled = terms.shuffled()
        val abbreviationPool = ArrayDeque(shuffled.filter { it.hasAbbreviation() })
        val otherPool = ArrayDeque(shuffled.filter { !it.hasAbbreviation() })

        val slotCounts = cardCountDistributor.distributeCounts(terms.size, slots.size)
        val variants = cardCountDistributor.flattenSlotVariants(slots, slotCounts)

        val resultsByIndex = arrayOfNulls<MutableList<Pair<T, TestType>>>(variants.size)
        for (variant in variants) resultsByIndex[variant.outputIndex] = mutableListOf()

        // 1단계: 약어 필요 변형부터 채우고, 못 채운 몫을 집계
        val shortfall = mutableMapOf<Int, Int>()
        for (variant in variants.sortedByDescending { it.testType.requiresAbbreviation() }) {
            var filled = 0
            repeat(variant.count) {
                val term = if (variant.testType.requiresAbbreviation()) {
                    abbreviationPool.removeFirstOrNull()
                } else {
                    otherPool.removeFirstOrNull() ?: abbreviationPool.removeFirstOrNull()
                }
                if (term != null) {
                    resultsByIndex[variant.outputIndex]!! += term to variant.testType
                    filled++
                }
            }
            if (filled < variant.count) shortfall[variant.outputIndex] = variant.count - filled
        }

        // 2단계: 약어 변형이 못 채운 몫을 약어 불필요 변형에 재배정
        val fillable = variants.filter { !it.testType.requiresAbbreviation() }
        if (fillable.isNotEmpty()) {
            var remaining = shortfall.values.sum()
            var i = 0
            while (remaining > 0) {
                val term = otherPool.removeFirstOrNull() ?: abbreviationPool.removeFirstOrNull() ?: break
                val target = fillable[i % fillable.size]
                resultsByIndex[target.outputIndex]!! += term to target.testType
                i++
                remaining--
            }
        }

        return resultsByIndex.asSequence().filterNotNull().flatten().toList()
    }

    private fun SimpleTermProjection.hasAbbreviation(): Boolean = !abbreviation.isNullOrBlank()
}