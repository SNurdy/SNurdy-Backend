package com.snurdy.backend.domain.test.legacy.service

import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.progress.dto.ProgressEvent
import com.snurdy.backend.domain.progress.entity.DailyProgressId
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.progress.service.ProgressUpdateService
import com.snurdy.backend.domain.test.legacy.dto.LevelUpEvent
import com.snurdy.backend.domain.test.legacy.dto.SubmitGlossaryTestResponse
import com.snurdy.backend.domain.test.legacy.dto.SubmitNoteTestResponse
import com.snurdy.backend.domain.test.legacy.dto.SubmitRequest
import com.snurdy.backend.domain.test.legacy.dto.TestGlossarySnapshot
import com.snurdy.backend.domain.user.customNote.service.CustomNoteService
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
class LegacyTestService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val customNoteService: CustomNoteService,
    private val wrongAnswerNoteService: WrongAnswerNoteService,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val progressUpdateService: ProgressUpdateService,
    private val clock: Clock
) {

    // POST /v2/test/glossaries/{glossaryId}/submit
    @Transactional
    fun submitGlossaryTest(
        userId: UUID,
        glossaryId: Long,
        request: SubmitRequest
    ): SubmitGlossaryTestResponse {
        progressUpdateService.lockUserForProgressUpdate(userId)

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
        val todayWasActiveBefore = todayBefore?.let { (it.newLearnedCount + it.allLearnedCount) > 0 } ?: false

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
        val studiedCounts = buildStudiedCounts(userId, request)
        return SubmitGlossaryTestResponse(
            correctCount = correctCount,
            incorrectCount = request.results.size - correctCount,
            sessionStudiedCount = studiedCounts.sessionStudiedCount,
            todayStudiedCount = studiedCounts.todayStudiedCount,
            totalStudiedCount = studiedCounts.totalStudiedCount,
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
        return buildSubmitNoteTestResponse(userId, request, correctCount)
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
        return buildSubmitNoteTestResponse(userId, request, correctCount)
    }

    private fun buildSubmitNoteTestResponse(
        userId: UUID,
        request: SubmitRequest,
        correctCount: Int
    ): SubmitNoteTestResponse {
        val studiedCounts = buildStudiedCounts(userId, request)
        return SubmitNoteTestResponse(
            correctCount = correctCount,
            incorrectCount = request.results.size - correctCount,
            sessionStudiedCount = studiedCounts.sessionStudiedCount,
            todayStudiedCount = studiedCounts.todayStudiedCount,
            totalStudiedCount = studiedCounts.totalStudiedCount
        )
    }

    // 노트 테스트는 진도 누적에 닿지 않으므로 today total 은 기본 단어장 학습 누적값 조회만
    private fun buildStudiedCounts(userId: UUID, request: SubmitRequest): StudiedCountResult {
        val today = clock.instant().atZone(APP_ZONE_ID).toLocalDate()
        val todayStudiedCount = dailyProgressRepository
            .findByIdOrNull(DailyProgressId(userId, today))
            ?.allLearnedCount ?: 0
        val totalStudiedCount = dailyProgressRepository.sumAllLearnedCountByUserId(userId).toInt()
        return StudiedCountResult(
            sessionStudiedCount = request.studiedTermCount ?: request.results.size,
            todayStudiedCount = todayStudiedCount,
            totalStudiedCount = totalStudiedCount
        )
    }

    private data class StudiedCountResult(
        val sessionStudiedCount: Int,
        val todayStudiedCount: Int,
        val totalStudiedCount: Int
    )

    private fun validateTermIdsBelongToGlossary(glossaryId: Long, termIds: List<Long>) {
        val requested = termIds.toSet()
        val matched = glossaryTermRepository
            .findAllByIdGlossaryIdAndIdTermIdIn(glossaryId, requested)
            .mapTo(mutableSetOf()) { it.id.termId }
        if (matched.size != requested.size) {
            throw ApiException(TestError.TEST_RESULT_INVALID)
        }
    }
}
