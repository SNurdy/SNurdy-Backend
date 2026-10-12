package com.snurdy.backend.domain.test.glossary.dto

import com.snurdy.backend.domain.progress.dto.ProgressEvent
import com.snurdy.backend.domain.test.common.dto.TestType
import com.snurdy.backend.domain.test.glossary.docs.GlossaryTestSwaggerDescription
import com.snurdy.backend.domain.test.glossary.docs.GlossaryTestSwaggerExample
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_REQUEST)
data class GlossaryTestSubmitRequest(
    @field:Valid
    @field:NotEmpty
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_REQUEST_RESULTS,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SUBMIT_REQUEST_RESULTS
    )
    val results: List<GlossaryTestResultItem>,

    @field:Positive
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_REQUEST_STUDIED_TERM_COUNT,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SUBMIT_REQUEST_STUDIED_TERM_COUNT
    )
    val studiedTermCount: Int? = null
)

@Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_RESULT_ITEM)
data class GlossaryTestResultItem(
    @field:Positive
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_RESULT_ITEM_TERM_ID,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_RESULT_ITEM_TERM_ID
    )
    val termId: Long,
    @field:NotNull
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_RESULT_ITEM_TEST_TYPE,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_RESULT_ITEM_TEST_TYPE
    )
    val testType: TestType,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_RESULT_ITEM_CORRECT,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_RESULT_ITEM_CORRECT
    )
    val correct: Boolean,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_RESULT_ITEM_USER_ANSWER,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_RESULT_ITEM_USER_ANSWER
    )
    val userAnswer: String
)

@Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_RESPONSE)
data class GlossaryTestSubmitResponse(
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_RESPONSE_CORRECT_COUNT,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SUBMIT_RESPONSE_CORRECT_COUNT
    )
    val correctCount: Int,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_RESPONSE_INCORRECT_COUNT,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SUBMIT_RESPONSE_INCORRECT_COUNT
    )
    val incorrectCount: Int,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_RESPONSE_SESSION_STUDIED_COUNT,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SUBMIT_RESPONSE_SESSION_STUDIED_COUNT
    )
    val sessionStudiedCount: Int,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_RESPONSE_TODAY_STUDIED_COUNT,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SUBMIT_RESPONSE_TODAY_STUDIED_COUNT
    )
    val todayStudiedCount: Int,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_RESPONSE_TOTAL_STUDIED_COUNT,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SUBMIT_RESPONSE_TOTAL_STUDIED_COUNT
    )
    val totalStudiedCount: Int,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_RESPONSE_TODAY_GOAL_COMPLETED,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SUBMIT_RESPONSE_TODAY_GOAL_COMPLETED
    )
    val todayGoalCompleted: Boolean,
    @field:Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SUBMIT_RESPONSE_GLOSSARY)
    val glossary: GlossaryTestSnapshot
)

@Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SNAPSHOT)
data class GlossaryTestSnapshot(
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SNAPSHOT_GLOSSARY_ID,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SNAPSHOT_GLOSSARY_ID
    )
    val glossaryId: Long,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SNAPSHOT_PROGRESS_RATE_BEFORE,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SNAPSHOT_PROGRESS_RATE_BEFORE
    )
    val progressRateBefore: Double,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SNAPSHOT_PROGRESS_RATE_AFTER,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SNAPSHOT_PROGRESS_RATE_AFTER
    )
    val progressRateAfter: Double,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SNAPSHOT_LEVEL_BEFORE,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_SNAPSHOT_LEVEL_BEFORE
    )
    val levelBefore: Int,
    @field:Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_SNAPSHOT_LEVEL_UP)
    val levelUp: GlossaryLevelUpEvent?
)

@Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_LEVEL_UP_EVENT)
data class GlossaryLevelUpEvent(
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_LEVEL_UP_EVENT_FROM,
        example = GlossaryTestSwaggerExample.GLOSSARY_LEVEL_UP_EVENT_FROM
    )
    val from: Int,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_LEVEL_UP_EVENT_TO,
        example = GlossaryTestSwaggerExample.GLOSSARY_LEVEL_UP_EVENT_TO
    )
    val to: Int
)

fun List<GlossaryTestResultItem>.toSubmitResponse(
    glossaryId: Long,
    studiedTermCount: Int?,
    before: GlossaryTestProgressSnapshot,
    after: GlossaryTestProgressSnapshot
): GlossaryTestSubmitResponse {
    val correctCount = count { it.correct }
    return GlossaryTestSubmitResponse(
        correctCount = correctCount,
        incorrectCount = size - correctCount,
        sessionStudiedCount = studiedTermCount ?: size,
        todayStudiedCount = after.todayStudiedCount,
        totalStudiedCount = after.totalStudiedCount,
        todayGoalCompleted = !before.activeToday,
        glossary = GlossaryTestSnapshot(
            glossaryId = glossaryId,
            progressRateBefore = before.progressRate,
            progressRateAfter = after.progressRate,
            levelBefore = before.level,
            levelUp = if (after.level > before.level) {
                GlossaryLevelUpEvent(from = before.level, to = after.level)
            } else null
        )
    )
}

// 시험 제출 전후 학습 상태를 같은 모양으로 비교하기 위한 한 시점의 지표 묶음
data class GlossaryTestProgressSnapshot(
    val progressRate: Double,
    val level: Int,
    val activeToday: Boolean,
    val todayStudiedCount: Int,
    val totalStudiedCount: Int
)

fun GlossaryTestResultItem.toProgressEvent(glossaryId: Long): ProgressEvent =
    ProgressEvent(
        glossaryId = glossaryId,
        termId = termId,
        correct = correct
    )