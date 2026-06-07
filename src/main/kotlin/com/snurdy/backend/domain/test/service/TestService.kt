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
import com.snurdy.backend.domain.test.dto.SubmitGlossaryTestResponse
import com.snurdy.backend.domain.test.dto.SubmitNoteTestResponse
import com.snurdy.backend.domain.test.dto.SubmitRequest
import com.snurdy.backend.domain.test.dto.TestGlossarySnapshot
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteTermRepository
import com.snurdy.backend.domain.user.customNote.service.CustomNoteService
import com.snurdy.backend.domain.user.wrongAnswerNote.repository.WrongAnswerTermRepository
import com.snurdy.backend.domain.user.wrongAnswerNote.service.WrongAnswerNoteService
import com.snurdy.backend.global.constants.APP_ZONE_ID
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class TestService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val customNoteService: CustomNoteService,
    private val customNoteTermRepository: CustomNoteTermRepository,
    private val wrongAnswerTermRepository: WrongAnswerTermRepository,
    private val wrongAnswerNoteService: WrongAnswerNoteService,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val progressUpdateService: ProgressUpdateService,
    private val clock: Clock
) {

    // POST /v2/test/glossaries/{glossaryId}/distractors
    @Transactional(readOnly = true)
    fun listGlossaryDistractors(
        userId: UUID,
        glossaryId: Long,
        request: DistractorsRequest
    ): DistractorsResponse {
        validateMultipleChoice(request.testType)
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
        val pool = glossaryTermRepository.findAllSimpleTermsByGlossaryId(glossaryId)
        return buildDistractorsResponse(request.testType, request.termIds, pool)
    }

    // POST /v2/test/custom-notes/{customNoteId}/distractors
    @Transactional(readOnly = true)
    fun listCustomNoteDistractors(
        userId: UUID,
        customNoteId: Long,
        request: DistractorsRequest
    ): DistractorsResponse {
        validateMultipleChoice(request.testType)
        customNoteService.requireNoteOwnership(userId, customNoteId)
        val pool: List<SimpleTermProjection> = customNoteTermRepository
            .findCardTermsByNoteId(noteId = customNoteId, requireAbbreviation = false)
        return buildDistractorsResponse(request.testType, request.termIds, pool)
    }

    // POST /v2/test/wrong-answer-notes/by-period/{period}/distractors
    // POST /v2/test/wrong-answer-notes/by-glossary/{glossaryId}/distractors
    // 두 endpoint 가 같은 흐름. termId 별 glossaryId 매핑 후 그 단어장의 다른 단어로 distractor 생성
    @Transactional(readOnly = true)
    fun listWrongAnswerDistractors(userId: UUID, request: DistractorsRequest): DistractorsResponse {
        validateMultipleChoice(request.testType)

        val distinctTermIds = request.termIds.distinct()
        val termGlossaryMap = wrongAnswerTermRepository
            .findGlossaryIdsByUserAndTermIds(userId, distinctTermIds)
            .associate { it.termId to it.glossaryId }
        if (termGlossaryMap.size != distinctTermIds.size) {
            throw ApiException(TestError.TEST_RESULT_INVALID)
        }

        val glossaryIds = termGlossaryMap.values.toSet()
        val poolByGlossaryId = glossaryIds.associateWith {
            glossaryTermRepository.findAllSimpleTermsByGlossaryId(it)
        }

        val distractorsMap = request.termIds.associateWith { termId ->
            val glossaryId = termGlossaryMap[termId]!!
            val pool = poolByGlossaryId[glossaryId].orEmpty()
            val target = pool.firstOrNull { it.termId == termId }
                ?: throw ApiException(TestError.TEST_RESULT_INVALID)
            val correctAnswer = deriveCorrectAnswer(target, request.testType)
                ?: throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
            buildDistractors(target, correctAnswer, request.testType, pool)
        }
        return DistractorsResponse(distractorsByTermId = distractorsMap)
    }

    // termIds 가 같은 1차 풀 안에 있을 때 공용 처리 (기본 단어장 / 커스텀노트)
    private fun buildDistractorsResponse(
        testType: TestType,
        termIds: List<Long>,
        pool: List<SimpleTermProjection>
    ): DistractorsResponse {
        val termById = pool.associateBy { it.termId }
        val distractorsMap = termIds.associateWith { termId ->
            val target = termById[termId]
                ?: throw ApiException(TestError.TEST_RESULT_INVALID)
            val correctAnswer = deriveCorrectAnswer(target, testType)
                ?: throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
            buildDistractors(target, correctAnswer, testType, pool)
        }
        return DistractorsResponse(distractorsByTermId = distractorsMap)
    }

    private fun validateMultipleChoice(testType: TestType) {
        if (!testType.isMultipleChoice()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)
    }

    private fun deriveCorrectAnswer(target: SimpleTermProjection, type: TestType): String? = when (type) {
        TestType.MC_PICK_TERM -> target.spelling
        TestType.MC_PICK_MEANING -> target.meaning
        TestType.MC_PICK_ABBR -> target.abbreviation?.takeIf { it.isNotBlank() }
        else -> null
    }

    // 1차 풀 우선, 부족하면 글로벌 random 으로 보충. 정답 텍스트와 중복되는 후보는 제외
    private fun buildDistractors(
        target: SimpleTermProjection,
        correctAnswer: String,
        type: TestType,
        primaryPool: List<SimpleTermProjection>
    ): List<String> {
        val fromPrimary = primaryPool.asSequence()
            .filter { it.termId != target.termId }
            .mapNotNull { extractText(it, type) }
            .filter { it != correctAnswer }
            .distinct()
            .shuffled()
            .take(MULTIPLE_CHOICE_DISTRACTOR_COUNT)
            .toList()

        if (fromPrimary.size >= MULTIPLE_CHOICE_DISTRACTOR_COUNT) return fromPrimary

        val needed = MULTIPLE_CHOICE_DISTRACTOR_COUNT - fromPrimary.size
        val taken = (fromPrimary + correctAnswer).toMutableSet()
        val excludeIds = primaryPool.mapTo(mutableSetOf()) { it.termId }
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

        val combined = fromPrimary + supplementary
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

    // POST /v2/test/glossaries/{glossaryId}/submit
    @Transactional
    fun submitGlossaryTest(
        userId: UUID,
        glossaryId: Long,
        request: SubmitRequest
    ): SubmitGlossaryTestResponse {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
        val termIds = request.results.map { it.termId }
        validateTermIdsBelongToGlossary(glossaryId, termIds)

        val totalTermCount = glossaryTermRepository.countByIdGlossaryId(glossaryId)
        val learnedBefore = glossaryProgressRepository
            .findByIdOrNull(GlossaryProgressId(userId, glossaryId))
            ?.learnedCount ?: 0
        val progressRateBefore = GlossaryProgressCalculator.progressRate(learnedBefore, totalTermCount)
        val levelBefore = GlossaryProgressCalculator.levelFromRate(progressRateBefore)

        val today = clock.instant().atZone(APP_ZONE_ID).toLocalDate()
        val todayBefore = dailyProgressRepository.findByIdOrNull(DailyProgressId(userId, today))
        val todayWasActiveBefore = todayBefore?.let { (it.learnedCount + it.completedCount) > 0 } ?: false

        val events = request.results.map { result ->
            ProgressEvent(
                glossaryId = glossaryId,
                termId = result.termId,
                correct = result.correct
            )
        }
        progressUpdateService.updateProgress(userId, events)

        val learnedAfter = glossaryProgressRepository
            .findByIdOrNull(GlossaryProgressId(userId, glossaryId))
            ?.learnedCount ?: learnedBefore
        val progressRateAfter = GlossaryProgressCalculator.progressRate(learnedAfter, totalTermCount)
        val levelAfter = GlossaryProgressCalculator.levelFromRate(progressRateAfter)

        val correctCount = request.results.count { it.correct }
        return SubmitGlossaryTestResponse(
            correctCount = correctCount,
            incorrectCount = request.results.size - correctCount,
            todayGoalCompleted = !todayWasActiveBefore,
            glossary = TestGlossarySnapshot(
                glossaryId = glossaryId,
                progressRateBefore = progressRateBefore,
                progressRateAfter = progressRateAfter,
                levelBefore = levelBefore,
                levelUp = if (levelAfter > levelBefore) LevelUpEvent(from = levelBefore, to = levelAfter) else null
            )
        )
    }

    // POST /v2/test/custom-notes/{customNoteId}/submit
    // 채점 집계만. 진도 시스템 안 닿고 오답 자동 기록도 안 함
    @Transactional(readOnly = true)
    fun submitCustomNoteTest(
        userId: UUID,
        customNoteId: Long,
        request: SubmitRequest
    ): SubmitNoteTestResponse {
        customNoteService.requireNoteOwnership(userId, customNoteId)
        val correctCount = request.results.count { it.correct }
        return SubmitNoteTestResponse(
            correctCount = correctCount,
            incorrectCount = request.results.size - correctCount
        )
    }

    // POST /v2/test/wrong-answer-notes/by-period/{period}/submit
    // POST /v2/test/wrong-answer-notes/by-glossary/{glossaryId}/submit
    // 채점 집계 + 또 틀린 단어의 누적값 증가. 진도 시스템 안 닿음
    @Transactional
    fun submitWrongAnswerTest(userId: UUID, request: SubmitRequest): SubmitNoteTestResponse {
        val incorrectTermIds = request.results.filter { !it.correct }.map { it.termId }
        if (incorrectTermIds.isNotEmpty()) {
            wrongAnswerNoteService.recordIncorrectsAgain(userId, incorrectTermIds, clock.instant())
        }
        val correctCount = request.results.count { it.correct }
        return SubmitNoteTestResponse(
            correctCount = correctCount,
            incorrectCount = request.results.size - correctCount
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
