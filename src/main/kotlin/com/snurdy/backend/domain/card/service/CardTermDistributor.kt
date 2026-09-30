package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.test.legacy.entity.TestType
import com.snurdy.backend.domain.test.legacy.entity.requiresAbbreviation
import org.springframework.stereotype.Component

// 주어진 단어 풀을 슬롯/변형에 1:1 매핑
// abbreviation 요구 변형부터 풀 우선 점유 — abbreviation 단어가 일반 변형에 흡수되어 약어 변형이 비는 사태 방지
// 슬롯별 가능 단어가 부족하면 그 변형만 카드 수 감소
// 카드 응답 순서는 입력 슬롯 순서 유지
@Component
class CardTermDistributor(
    private val cardCountDistributor: CardCountDistributor
) {

    fun <T : SimpleTermProjection> distribute(
        terms: List<T>,
        slots: List<CardSlot>
    ): List<Pair<T, TestType>> {
        if (slots.isEmpty() || terms.isEmpty()) return emptyList()
        val shuffled = terms.shuffled()
        val abbreviationPool = ArrayDeque(shuffled.filter { it.hasAbbreviation() })
        val otherPool = ArrayDeque(shuffled.filter { !it.hasAbbreviation() })

        val slotCounts = cardCountDistributor.distributeCounts(terms.size, slots.size)
        val variants = cardCountDistributor.flattenSlotVariants(slots, slotCounts)

        val resultsByIndex = arrayOfNulls<MutableList<Pair<T, TestType>>>(variants.size)
        for (variant in variants) resultsByIndex[variant.outputIndex] = mutableListOf()

        for (variant in variants.sortedByDescending { it.testType.requiresAbbreviation() }) {
            repeat(variant.count) {
                val term = if (variant.testType.requiresAbbreviation()) {
                    abbreviationPool.removeFirstOrNull()
                } else {
                    otherPool.removeFirstOrNull() ?: abbreviationPool.removeFirstOrNull()
                }
                if (term != null) resultsByIndex[variant.outputIndex]!! += term to variant.testType
            }
        }
        return resultsByIndex.asSequence().filterNotNull().flatten().toList()
    }

    private fun SimpleTermProjection.hasAbbreviation(): Boolean = !abbreviation.isNullOrBlank()
}
