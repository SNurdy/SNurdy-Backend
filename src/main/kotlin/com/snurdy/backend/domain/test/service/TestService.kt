package com.snurdy.backend.domain.test.service

import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.progress.dto.ProgressEvent
import com.snurdy.backend.domain.progress.entity.DailyProgressId
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.progress.service.ProgressUpdateService
import com.snurdy.backend.domain.test.dto.LevelUpEvent
import com.snurdy.backend.domain.test.dto.SubmitTestRequest
import com.snurdy.backend.domain.test.dto.SubmitTestResponse
import com.snurdy.backend.domain.test.dto.TestGlossarySnapshot
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
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
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val progressUpdateService: ProgressUpdateService,
    private val clock: Clock
) {

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
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
