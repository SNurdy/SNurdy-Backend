package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.service.MorphemeHighlightAssembler
import com.snurdy.backend.domain.progress.dto.ProgressEvent
import com.snurdy.backend.domain.progress.entity.DailyProgressId
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.progress.service.ProgressUpdateService
import com.snurdy.backend.domain.test.common.entity.TestSubmissionType
import com.snurdy.backend.domain.test.common.service.TestSubmissionService
import com.snurdy.backend.domain.test.glossary.dto.GlossaryLevelUpEvent
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSnapshot
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitResponse
import com.snurdy.backend.global.constants.APP_ZONE_ID
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID
import java.time.Clock

@Service
class GlossaryTestSubmitService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val cardCountDistributor: GlossaryCardCountDistributor,
    private val cardTermDistributor: GlossaryCardTermDistributor,
    private val distractorSelector: GlossaryDistractorSelector,
    private val morphemeHighlightAssembler: MorphemeHighlightAssembler,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val progressUpdateService: ProgressUpdateService,
    private val testSubmissionService: TestSubmissionService,
    private val clock: Clock
) {
    @Transactional
    fun submitTest(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryTestSubmitRequest,
        idempotencyKey: UUID?
    ): GlossaryTestSubmitResponse {
        progressUpdateService.lockUserForProgressUpdate(userId)

        testSubmissionService
            .findPreviousResponse(userId, idempotencyKey, GlossaryTestSubmitResponse::class.java)
            ?.let { return it }

        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
        val results = request.results.distinctBy { it.termId }
        val termIds = results.map { it.termId }
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

        val events = results.map {
            ProgressEvent(glossaryId = glossaryId, termId = it.termId, correct = it.correct)
        }
        progressUpdateService.updateProgress(userId, events)

        val learnedAfter = glossaryProgressRepository
            .findByIdOrNull(GlossaryProgressId(userId, glossaryId))
            ?.learnedCount ?: learnedBefore
        val progressRateAfter = GlossaryProgressCalculator.progressRate(learnedAfter, totalTermCount)
        val levelAfter = GlossaryProgressCalculator.levelFromRate(progressRateAfter)

        val todayStudiedCount = dailyProgressRepository
            .findByIdOrNull(DailyProgressId(userId, today))
            ?.allLearnedCount ?: 0
        val totalStudiedCount = dailyProgressRepository.sumAllLearnedCountByUserId(userId).toInt()

        val correctCount = results.count { it.correct }
        val response = GlossaryTestSubmitResponse(
            correctCount = correctCount,
            incorrectCount = results.size - correctCount,
            sessionStudiedCount = request.studiedTermCount ?: results.size,
            todayStudiedCount = todayStudiedCount,
            totalStudiedCount = totalStudiedCount,
            todayGoalCompleted = !todayWasActiveBefore,
            glossary = GlossaryTestSnapshot(
                glossaryId = glossaryId,
                progressRateBefore = progressRateBefore,
                progressRateAfter = progressRateAfter,
                levelBefore = levelBefore,
                levelUp = if (levelAfter > levelBefore) GlossaryLevelUpEvent(
                    from = levelBefore,
                    to = levelAfter
                ) else null
            )
        )

        testSubmissionService.saveResponse(userId, idempotencyKey, TestSubmissionType.GLOSSARY, response)
        return response
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
}