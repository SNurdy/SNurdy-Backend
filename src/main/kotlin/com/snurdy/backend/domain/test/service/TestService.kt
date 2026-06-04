package com.snurdy.backend.domain.test.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.test.dto.StartTestRequest
import com.snurdy.backend.domain.test.dto.StartTestResponse
import com.snurdy.backend.domain.test.dto.TestPrompt
import com.snurdy.backend.domain.test.dto.TestQuestion
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.LearningError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TestService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository
) {

    @Transactional(readOnly = true)
    fun startTest(userId: UUID, request: StartTestRequest): StartTestResponse {
        if (!glossaryRepository.existsById(request.glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val allTerms = glossaryTermRepository.findAllSimpleTermsByGlossaryId(request.glossaryId)
        val eligibleTerms = filterEligibleTerms(allTerms, request.testType)
        validateAvailability(allTerms, eligibleTerms, request.testType)

        val sampled = eligibleTerms.shuffled().take(request.questionCount)
        val questions = sampled.map { target -> buildQuestion(target, request.testType, allTerms) }

        return StartTestResponse(
            glossaryId = request.glossaryId,
            testType = request.testType,
            questions = questions
        )
    }

    private fun filterEligibleTerms(all: List<SimpleTermProjection>, type: TestType): List<SimpleTermProjection> =
        when (type) {
            TestType.WRITE_ABBREVIATION, TestType.MC_PICK_ABBR -> all.filter { !it.abbreviation.isNullOrBlank() }
            else -> all
        }

    private fun validateAvailability(
        all: List<SimpleTermProjection>,
        eligible: List<SimpleTermProjection>,
        type: TestType
    ) {
        if (eligible.isEmpty()) throw ApiException(LearningError.TEST_TYPE_NOT_AVAILABLE)
        if (type.isMultipleChoice()) {
            // 다지선다는 정답 1개 + 오답 3개 보기를 위해 후보가 4개 이상 필요
            val distractorPool = if (type == TestType.MC_PICK_ABBR) eligible else all
            if (distractorPool.size < MULTIPLE_CHOICE_MIN) {
                throw ApiException(LearningError.TEST_TYPE_NOT_AVAILABLE)
            }
        }
    }

    private fun buildQuestion(
        target: SimpleTermProjection,
        type: TestType,
        pool: List<SimpleTermProjection>
    ): TestQuestion {
        val (promptText, correctAnswer, choices) = when (type) {
            TestType.TERM_TO_MEANING -> Triple(target.spelling, target.meaning, null)
            TestType.MEANING_TO_TERM -> Triple(target.meaning, target.spelling, null)
            TestType.LISTEN_AND_WRITE -> Triple(target.spelling, target.spelling, null)
            TestType.WRITE_ABBREVIATION -> Triple(target.spelling, target.abbreviation!!, null)
            TestType.MC_PICK_TERM -> {
                val correct = target.spelling
                val distractors = pool.asSequence()
                    .filter { it.termId != target.termId }
                    .map { it.spelling }
                    .distinct()
                    .shuffled()
                    .take(MULTIPLE_CHOICE_DISTRACTOR_COUNT)
                    .toList()
                Triple(target.meaning, correct, (distractors + correct).shuffled())
            }
            TestType.MC_PICK_MEANING -> {
                val correct = target.meaning
                val distractors = pool.asSequence()
                    .filter { it.termId != target.termId }
                    .map { it.meaning }
                    .distinct()
                    .shuffled()
                    .take(MULTIPLE_CHOICE_DISTRACTOR_COUNT)
                    .toList()
                Triple(target.spelling, correct, (distractors + correct).shuffled())
            }
            TestType.MC_PICK_ABBR -> {
                val correct = target.abbreviation!!
                val distractors = pool.asSequence()
                    .filter { it.termId != target.termId && !it.abbreviation.isNullOrBlank() }
                    .map { it.abbreviation!! }
                    .distinct()
                    .shuffled()
                    .take(MULTIPLE_CHOICE_DISTRACTOR_COUNT)
                    .toList()
                Triple(target.spelling, correct, (distractors + correct).shuffled())
            }
        }
        return TestQuestion(
            termId = target.termId,
            prompt = TestPrompt(type = type, text = promptText),
            choices = choices,
            correctAnswer = correctAnswer
        )
    }

    companion object {
        private const val MULTIPLE_CHOICE_MIN = 4
        private const val MULTIPLE_CHOICE_DISTRACTOR_COUNT = 3
    }
}

private fun TestType.isMultipleChoice(): Boolean = when (this) {
    TestType.MC_PICK_TERM, TestType.MC_PICK_MEANING, TestType.MC_PICK_ABBR -> true
    else -> false
}
