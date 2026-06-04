package com.snurdy.backend.domain.test.service

import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.dto.LearningAnswerEvent
import com.snurdy.backend.domain.learning.dto.LearningSubmitRequest
import com.snurdy.backend.domain.learning.entity.DailyProgressId
import com.snurdy.backend.domain.learning.entity.GlossaryProgressId
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.service.LearningCommandService
import com.snurdy.backend.domain.test.dto.LevelUpEvent
import com.snurdy.backend.domain.test.dto.StartTestRequest
import com.snurdy.backend.domain.test.dto.StartTestResponse
import com.snurdy.backend.domain.test.dto.SubmitTestRequest
import com.snurdy.backend.domain.test.dto.SubmitTestResponse
import com.snurdy.backend.domain.test.dto.TestGlossarySnapshot
import com.snurdy.backend.domain.test.dto.TestPrompt
import com.snurdy.backend.domain.test.dto.TestQuestion
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.LearningError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZoneId
import java.util.UUID

@Service
class TestService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val learningCommandService: LearningCommandService,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun startTest(userId: UUID, request: StartTestRequest): StartTestResponse {
        if (!glossaryRepository.existsById(request.glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val allTerms = glossaryTermRepository.findAllSimpleTermsByGlossaryId(request.glossaryId)
        val eligibleTerms = filterEligibleTerms(allTerms, request.testType)
        if (eligibleTerms.isEmpty()) throw ApiException(LearningError.TEST_TYPE_NOT_AVAILABLE)

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
                val choices = buildMultipleChoiceOptions(
                    target = target,
                    pool = pool,
                    correctAnswer = correct,
                    requireAbbreviation = false,
                    sameGlossaryExtractor = { it.spelling },
                    distractorExtractor = { it.spelling }
                )
                Triple(target.meaning, correct, choices)
            }
            TestType.MC_PICK_MEANING -> {
                val correct = target.meaning
                val choices = buildMultipleChoiceOptions(
                    target = target,
                    pool = pool,
                    correctAnswer = correct,
                    requireAbbreviation = false,
                    sameGlossaryExtractor = { it.meaning },
                    distractorExtractor = { it.meaning }
                )
                Triple(target.spelling, correct, choices)
            }
            TestType.MC_PICK_ABBR -> {
                val correct = target.abbreviation!!
                val choices = buildMultipleChoiceOptions(
                    target = target,
                    pool = pool,
                    correctAnswer = correct,
                    requireAbbreviation = true,
                    sameGlossaryExtractor = { it.abbreviation?.takeIf { value -> value.isNotBlank() } },
                    distractorExtractor = { it.abbreviation?.takeIf { value -> value.isNotBlank() } }
                )
                Triple(target.spelling, correct, choices)
            }
        }
        return TestQuestion(
            termId = target.termId,
            prompt = TestPrompt(type = type, text = promptText),
            choices = choices,
            correctAnswer = correctAnswer
        )
    }

    // 같은 단어장에서 최대한 보기를 채우고, 부족하면 다른 단어장 random pool 에서 보충
    private fun buildMultipleChoiceOptions(
        target: SimpleTermProjection,
        pool: List<SimpleTermProjection>,
        correctAnswer: String,
        requireAbbreviation: Boolean,
        sameGlossaryExtractor: (SimpleTermProjection) -> String?,
        distractorExtractor: (com.snurdy.backend.domain.dictionary.term.repository.DistractorTermProjection) -> String?
    ): List<String> {
        val sameGlossary = pool.asSequence()
            .filter { it.termId != target.termId }
            .mapNotNull(sameGlossaryExtractor)
            .filter { it != correctAnswer }
            .distinct()
            .shuffled()
            .take(MULTIPLE_CHOICE_DISTRACTOR_COUNT)
            .toList()

        val needed = MULTIPLE_CHOICE_DISTRACTOR_COUNT - sameGlossary.size
        val distractors = if (needed <= 0) {
            sameGlossary
        } else {
            val taken = (sameGlossary + correctAnswer).toMutableSet()
            val excludeIds = (pool.map { it.termId } + target.termId).toSet()
            val supplementary = mutableListOf<String>()
            val fetched = termRepository.findRandomDistractorTerms(
                excludeTermIds = excludeIds,
                requireAbbreviation = requireAbbreviation,
                limit = needed * SUPPLEMENT_FETCH_MULTIPLIER
            )
            for (candidate in fetched) {
                val text = distractorExtractor(candidate) ?: continue
                if (text in taken) continue
                taken += text
                supplementary += text
                if (supplementary.size == needed) break
            }
            sameGlossary + supplementary
        }

        return (distractors + correctAnswer).shuffled()
    }

    @Transactional
    fun submitTest(userId: UUID, request: SubmitTestRequest): SubmitTestResponse {
        if (!glossaryRepository.existsById(request.glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val termIds = request.results.map { it.termId }
        validateTermIdsBelongToGlossary(request.glossaryId, termIds)

        val totalTermCount = glossaryTermRepository.countByIdGlossaryId(request.glossaryId)
        val learnedBefore = glossaryProgressRepository
            .findByIdOrNull(GlossaryProgressId(userId, request.glossaryId))
            ?.learnedCount ?: 0
        val progressRateBefore = GlossaryProgressCalculator.progressRate(learnedBefore, totalTermCount)
        val levelBefore = GlossaryProgressCalculator.levelFromRate(progressRateBefore)

        val today = clock.instant().atZone(APP_ZONE_ID).toLocalDate()
        val todayBefore = dailyProgressRepository.findByIdOrNull(DailyProgressId(userId, today))
        val todayWasActiveBefore = todayBefore?.let { (it.learnedCount + it.completedCount) > 0 } ?: false

        val learningRequest = LearningSubmitRequest(
            events = request.results.map { result ->
                LearningAnswerEvent(
                    glossaryId = request.glossaryId,
                    termId = result.termId,
                    correct = result.correct
                )
            }
        )
        learningCommandService.submitLearning(userId, learningRequest)

        val learnedAfter = glossaryProgressRepository
            .findByIdOrNull(GlossaryProgressId(userId, request.glossaryId))
            ?.learnedCount ?: learnedBefore
        val progressRateAfter = GlossaryProgressCalculator.progressRate(learnedAfter, totalTermCount)
        val levelAfter = GlossaryProgressCalculator.levelFromRate(progressRateAfter)

        val correctCount = request.results.count { it.correct }
        return SubmitTestResponse(
            correctCount = correctCount,
            incorrectCount = request.results.size - correctCount,
            todayGoalCompleted = !todayWasActiveBefore,
            glossary = TestGlossarySnapshot(
                glossaryId = request.glossaryId,
                progressRateBefore = progressRateBefore,
                progressRateAfter = progressRateAfter,
                levelBefore = levelBefore,
                levelUp = if (levelAfter > levelBefore) LevelUpEvent(from = levelBefore, to = levelAfter) else null
            )
        )
    }

    private fun validateTermIdsBelongToGlossary(glossaryId: Long, termIds: List<Long>) {
        val requested = termIds.toSet()
        val matched = glossaryTermRepository
            .findAllByIdGlossaryIdAndIdTermIdIn(glossaryId, requested)
            .mapTo(mutableSetOf()) { it.id.termId }
        if (matched.size != requested.size) {
            throw ApiException(LearningError.TEST_RESULT_INVALID)
        }
    }

    companion object {
        private const val MULTIPLE_CHOICE_DISTRACTOR_COUNT = 3
        private const val SUPPLEMENT_FETCH_MULTIPLIER = 5
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
