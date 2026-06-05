package com.snurdy.backend.domain.test.service

import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.progress.dto.ProgressEvent
import com.snurdy.backend.domain.progress.entity.DailyProgressId
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.progress.service.ProgressUpdateService
import com.snurdy.backend.domain.test.dto.DistractorsRequest
import com.snurdy.backend.domain.test.dto.DistractorsResponse
import com.snurdy.backend.domain.test.dto.LevelUpEvent
import com.snurdy.backend.domain.test.dto.SubmitTestRequest
import com.snurdy.backend.domain.test.dto.SubmitTestResponse
import com.snurdy.backend.domain.test.dto.TestGlossarySnapshot
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import com.snurdy.backend.global.constants.APP_ZONE_ID
import java.util.UUID

@Service
class TestService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val progressUpdateService: ProgressUpdateService,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun listDistractors(userId: UUID, request: DistractorsRequest): DistractorsResponse {
        if (!request.testType.isMultipleChoice()) {
            throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
        }
        if (!glossaryRepository.existsById(request.glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val glossaryTerms = glossaryTermRepository.findAllSimpleTermsByGlossaryId(request.glossaryId)
        val termById = glossaryTerms.associateBy { it.termId }

        val distractorsMap = request.termIds.associateWith { termId ->
            val target = termById[termId]
                ?: throw ApiException(TestError.TEST_RESULT_INVALID)
            val correctAnswer = deriveCorrectAnswer(target, request.testType)
                ?: throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
            buildDistractors(target, correctAnswer, request.testType, glossaryTerms)
        }

        return DistractorsResponse(distractorsByTermId = distractorsMap)
    }

    private fun deriveCorrectAnswer(target: SimpleTermProjection, type: TestType): String? = when (type) {
        TestType.MC_PICK_TERM -> target.spelling
        TestType.MC_PICK_MEANING -> target.meaning
        TestType.MC_PICK_ABBR -> target.abbreviation?.takeIf { it.isNotBlank() }
        else -> null
    }

    // 같은 단어장 우선, 부족하면 다른 단어장 random pool 에서 보충
    // 정답 텍스트와 중복되는 후보는 모두 제외 (정답 중복 보장)
    private fun buildDistractors(
        target: SimpleTermProjection,
        correctAnswer: String,
        type: TestType,
        glossaryPool: List<SimpleTermProjection>
    ): List<String> {
        val sameGlossary = glossaryPool.asSequence()
            .filter { it.termId != target.termId }
            .mapNotNull { extractText(it, type) }
            .filter { it != correctAnswer }
            .distinct()
            .shuffled()
            .take(MULTIPLE_CHOICE_DISTRACTOR_COUNT)
            .toList()

        if (sameGlossary.size >= MULTIPLE_CHOICE_DISTRACTOR_COUNT) return sameGlossary

        val needed = MULTIPLE_CHOICE_DISTRACTOR_COUNT - sameGlossary.size
        val taken = (sameGlossary + correctAnswer).toMutableSet()
        val excludeIds = glossaryPool.mapTo(mutableSetOf()) { it.termId }
        val supplementary = mutableListOf<String>()
        val fetched = termRepository.findRandomDistractorTerms(
            excludeTermIds = excludeIds,
            requireAbbreviation = type == TestType.MC_PICK_ABBR,
            limit = needed * SUPPLEMENT_FETCH_MULTIPLIER
        )
        for (candidate in fetched) {
            val text = extractTextFromDistractor(candidate, type) ?: continue
            if (text in taken) continue
            taken += text
            supplementary += text
            if (supplementary.size == needed) break
        }

        val combined = sameGlossary + supplementary
        if (combined.size < MULTIPLE_CHOICE_DISTRACTOR_COUNT) {
            throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
        }
        return combined
    }

    private fun extractText(term: SimpleTermProjection, type: TestType): String? = when (type) {
        TestType.MC_PICK_TERM -> term.spelling
        TestType.MC_PICK_MEANING -> term.meaning
        TestType.MC_PICK_ABBR -> term.abbreviation?.takeIf { it.isNotBlank() }
        else -> null
    }

    private fun extractTextFromDistractor(
        term: com.snurdy.backend.domain.dictionary.term.repository.DistractorTermProjection,
        type: TestType
    ): String? = when (type) {
        TestType.MC_PICK_TERM -> term.spelling
        TestType.MC_PICK_MEANING -> term.meaning
        TestType.MC_PICK_ABBR -> term.abbreviation?.takeIf { it.isNotBlank() }
        else -> null
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

        val events = request.results.map { result ->
            ProgressEvent(
                glossaryId = request.glossaryId,
                termId = result.termId,
                correct = result.correct
            )
        }
        progressUpdateService.updateProgress(userId, events)

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
            throw ApiException(TestError.TEST_RESULT_INVALID)
        }
    }

    companion object {
        private const val MULTIPLE_CHOICE_DISTRACTOR_COUNT = 3
        private const val SUPPLEMENT_FETCH_MULTIPLIER = 5
    }
}

private fun TestType.isMultipleChoice(): Boolean = when (this) {
    TestType.MC_PICK_TERM, TestType.MC_PICK_MEANING, TestType.MC_PICK_ABBR -> true
    else -> false
}
