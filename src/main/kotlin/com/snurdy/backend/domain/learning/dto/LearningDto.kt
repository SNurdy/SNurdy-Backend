package com.snurdy.backend.domain.learning.dto

import com.snurdy.backend.domain.dictionary.glossary.repository.NewLearningCardProjection
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.learning.service.SrsCalculator
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
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
)

@Schema(description = "학습 결과 제출 요청. 한 번의 제출에는 단일 단어장의 학습 이벤트들만 포함되어야 한다")
data class LearningSubmitRequest(
    @field:Valid
    @field:NotEmpty(message = "학습 이벤트가 비어 있을 수 없습니다")
    @field:Schema(description = "학습 이벤트 목록 (한 단어=한 이벤트)")
    val events: List<LearningAnswerEvent>
)

@Schema(description = "학습 이벤트 (한 단어에 대한 정답/오답 결과)")
data class LearningAnswerEvent(
    @field:Positive
    @field:Schema(description = "학습 중인 단어장 ID", example = "12")
    val glossaryId: Long,
    @field:Positive
    @field:Schema(description = "학습한 단어 ID", example = "586")
    val termId: Long,
    @field:Schema(description = "정답 여부 (클라이언트가 사용자 답과 정답을 비교해서 결정)", example = "true")
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

// 기존 진행도가 있는 용어 카드 변환 (복습/학습중)
fun TermProgressEntity.toResponse(term: TermEntity, bookmarked: Boolean) = LearningTermCard(
    termId = term.id,
    spelling = term.spelling,
    meaning = term.meaning,
    abbreviation = term.abbreviation,
    status = status.name,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    reviewCount = reviewCount,
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
    bookmarked = bookmarked
)

// 아직 학습 시작 전(NEW) 용어 카드 변환. SRS 기본값 채워서 반환
fun NewLearningCardProjection.toResponse() = LearningTermCard(
    termId = termId,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    status = TermProgressStatus.NEW.name,
    easeFactor = SrsCalculator.DEFAULT_EASE_FACTOR,
    intervalDays = 0,
    reviewCount = 0,
    lastReviewedAt = null,
    nextReviewAt = null,
    bookmarked = bookmarked == 1
)
