package com.snurdy.backend.domain.learning.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class StudyNextTermsResponse(
    val glossaryId: Long,
    val terms: List<LearningTermCard>
)

data class ReviewDueTermsResponse(
    val glossaryId: Long,
    val terms: List<LearningTermCard>
)

data class LearningTermCard(
    val termId: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val status: String,
    val easeFactor: BigDecimal,
    val intervalDays: Int,
    val reviewCount: Int,
    val lastReviewedAt: Instant?,
    val nextReviewAt: Instant?,
    val bookmarked: Boolean
)

data class LearningSubmitRequest(
    @field:Valid
    @field:NotEmpty
    val events: List<LearningAnswerEvent>
)

data class LearningAnswerEvent(
    @field:Positive
    val glossaryId: Long,
    @field:Positive
    val termId: Long,
    val correct: Boolean
)

data class LearningSubmitResponse(
    val processedCount: Int,
    val learnedCount: Int,
    val reviewedCount: Int
)

data class CalendarStatsResponse(
    val year: Int,
    val month: Int,
    val days: List<CalendarDailyItem>,
    val totalLearnedCount: Int,
    val totalReviewedCount: Int
)

data class CalendarDailyItem(
    val date: LocalDate,
    val learnedCount: Int,
    val reviewedCount: Int
)

data class UserStreakResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalLearningTerms: Int
)
