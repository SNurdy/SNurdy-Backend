package com.snurdy.backend.domain.learning.dto

import com.snurdy.backend.domain.dictionary.glossary.repository.NewLearningCardProjection
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.learning.service.SrsCalculator
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.Instant

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

@Schema(description = "학습 제출 응답. learnedCount + reviewedCount = processedCount 가 항상 성립한다")
data class LearningSubmitResponse(
    @field:Schema(description = "처리된 이벤트 총 개수")
    val processedCount: Int,
    @field:Schema(description = "정답으로 푼 이벤트 개수")
    val correctCount: Int,
    @field:Schema(description = "오답으로 푼 이벤트 개수")
    val incorrectCount: Int,
    @field:Schema(description = "처음으로 학습된 단어 개수 (NEW -> LEARNING 전환)")
    val learnedCount: Int,
    @field:Schema(description = "이미 학습된 단어를 복습한 이벤트 개수 (= processedCount - learnedCount)")
    val reviewedCount: Int,
    @field:Schema(description = "이번 제출로 단어장 레벨이 올랐을 때만 채워지는 이벤트, 변화 없으면 null")
    val levelUp: GlossaryLevelUp?
)

data class GlossaryLevelUp(
    val glossaryId: Long,
    val title: String,
    val previousLevel: Int,
    val newLevel: Int
)

