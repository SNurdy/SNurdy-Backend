package com.snurdy.backend.domain.learning.dto

import com.snurdy.backend.domain.dictionary.glossary.repository.NewLearningCardProjection
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.learning.service.SrsCalculator
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
) {
    companion object {
        // 기존 진행도가 있는 용어를 카드로 변환 (복습/학습중 상태 등)
        fun from(progress: TermProgressEntity, term: TermEntity, bookmarked: Boolean) = LearningTermCard(
            termId = term.id,
            spelling = term.spelling,
            meaning = term.meaning,
            abbreviation = term.abbreviation,
            status = progress.status.name,
            easeFactor = progress.easeFactor,
            intervalDays = progress.intervalDays,
            reviewCount = progress.reviewCount,
            lastReviewedAt = progress.lastReviewedAt,
            nextReviewAt = progress.nextReviewAt,
            bookmarked = bookmarked
        )

        // 아직 학습 시작 전(NEW)인 용어를 카드로 변환. SRS 기본값 채워서 반환.
        fun fromNewProjection(projection: NewLearningCardProjection) = LearningTermCard(
            termId = projection.termId,
            spelling = projection.spelling,
            meaning = projection.meaning,
            abbreviation = projection.abbreviation,
            status = TermProgressStatus.NEW.name,
            easeFactor = SrsCalculator.DEFAULT_EASE_FACTOR,
            intervalDays = 0,
            reviewCount = 0,
            lastReviewedAt = null,
            nextReviewAt = null,
            bookmarked = projection.bookmarked == 1
        )
    }
}

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
