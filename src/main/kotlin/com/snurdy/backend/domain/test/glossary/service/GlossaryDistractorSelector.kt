package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.dictionary.term.repository.DistractorTermProjection
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.test.common.TestType
import com.snurdy.backend.domain.test.common.multipleChoiceFieldOf
import org.springframework.stereotype.Component

@Component
class GlossaryDistractorSelector(
    private val termRepository: TermRepository
) {

    fun select(
        targetTermId: Long,
        correctAnswer: String,
        testType: TestType,
        primaryPool: List<SimpleTermProjection>
    ): List<String> {
        val fromPrimary = primaryPool.asSequence()
            .filter { it.termId != targetTermId }
            .mapNotNull { it.multipleChoiceText(testType) }
            .filter { it != correctAnswer }
            .distinct()
            .shuffled()
            .take(MULTIPLE_CHOICE_DISTRACTOR_COUNT)
            .toList()

        if (fromPrimary.size >= MULTIPLE_CHOICE_DISTRACTOR_COUNT) return fromPrimary

        val needed = MULTIPLE_CHOICE_DISTRACTOR_COUNT - fromPrimary.size
        val taken = (fromPrimary + correctAnswer).toMutableSet()
        val excludeIds = primaryPool.mapTo(mutableSetOf()) { it.termId }
        if (excludeIds.isEmpty()) excludeIds += targetTermId
        val fetched = termRepository.findRandomDistractorTerms(
            excludeTermIds = excludeIds,
            requireAbbreviation = testType == TestType.MC_TERM_FROM_ABBREVIATION,
            limit = needed * SUPPLEMENT_FETCH_MULTIPLIER
        )
        val supplementary = mutableListOf<String>()
        for (candidate in fetched) {
            val text = candidate.multipleChoiceText(testType) ?: continue
            if (text in taken) continue
            taken += text
            supplementary += text
            if (supplementary.size == needed) break
        }
        return fromPrimary + supplementary
    }

    private fun SimpleTermProjection.multipleChoiceText(testType: TestType): String? =
        testType.multipleChoiceFieldOf(spelling, meaning, abbreviation)?.takeIf { it.isNotBlank() }

    private fun DistractorTermProjection.multipleChoiceText(testType: TestType): String? =
        testType.multipleChoiceFieldOf(spelling, meaning, abbreviation)?.takeIf { it.isNotBlank() }

    companion object {
        private const val MULTIPLE_CHOICE_DISTRACTOR_COUNT = 3
        private const val SUPPLEMENT_FETCH_MULTIPLIER = 5
    }
}