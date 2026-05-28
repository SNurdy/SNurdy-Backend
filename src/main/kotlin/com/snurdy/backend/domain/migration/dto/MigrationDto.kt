package com.snurdy.backend.domain.migration.dto

import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.migration.entity.MigrationSessionEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

@Schema(description = "v1 로컬 데이터 일괄 업로드 요청. 한 번에 모든 학습 상태/북마크/일별 진행/요약을 전송")
data class MigrationUploadRequest(
    @field:Valid
    @field:Size(max = 5000)
    @field:Schema(description = "단어별 학습 진행 상태 목록 (최대 5000개)")
    val termProgresses: List<TermProgressPayload>,
    @field:Valid
    @field:Size(max = 1000)
    @field:Schema(description = "단어장별 학습 진행 요약 (최대 1000개)")
    val glossaryProgresses: List<GlossaryProgressPayload>,
    @field:Valid
    @field:Size(max = 5000)
    @field:Schema(description = "북마크 목록 (최대 5000개)")
    val bookmarks: List<BookmarkPayload> = emptyList(),
    @field:Valid
    @field:Size(max = 3650)
    @field:Schema(description = "일자별 학습 카운트 (최대 3650일)")
    val dailyProgresses: List<DailyProgressPayload> = emptyList(),
    @field:Valid
    @field:Schema(description = "사용자 전체 요약 (streak/총 학습 단어수)")
    val userSummary: UserSummaryPayload? = null
)

@Schema(description = "단어별 학습 진행 상태")
data class TermProgressPayload(
    @field:Positive
    @field:Schema(description = "단어장 ID", example = "12")
    val glossaryId: Long,
    @field:Positive
    @field:Schema(description = "단어 ID", example = "586")
    val termId: Long,
    @field:Schema(description = "학습 상태", example = "REVIEWING")
    val status: TermProgressStatus,
    @field:DecimalMin("1.0")
    @field:DecimalMax("9.0")  // SM-2 알고리즘 실용 범위
    @field:Schema(description = "SM-2 ease factor (1.0~9.0)", example = "2.5")
    val easeFactor: Double,
    @field:Min(0)
    @field:Schema(description = "다음 복습까지 간격(일)", example = "3")
    val intervalDays: Int,
    @field:Min(0)
    @field:Schema(description = "총 복습 횟수", example = "4")
    val reviewCount: Int,
    @field:Schema(description = "마지막 복습 시각 (ISO-8601)", example = "2026-05-20T10:15:30Z")
    val lastReviewedAt: Instant?,
    @field:Schema(description = "다음 복습 예정 시각 (ISO-8601)", example = "2026-05-23T10:15:30Z")
    val nextReviewAt: Instant?
)

@Schema(description = "단어장별 학습 진행 요약")
data class GlossaryProgressPayload(
    @field:Positive
    @field:Schema(description = "단어장 ID", example = "12")
    val glossaryId: Long,
    @field:Min(0)
    @field:Schema(description = "학습 완료한 단어 수", example = "37")
    val learnedCount: Int,
    @field:Schema(description = "마지막 학습 시각", example = "2026-05-20T10:15:30Z")
    val lastLearnedAt: Instant?
)

@Schema(description = "북마크 항목")
data class BookmarkPayload(
    @field:Positive
    @field:Schema(description = "단어장 ID", example = "12")
    val glossaryId: Long,
    @field:Positive
    @field:Schema(description = "단어 ID", example = "586")
    val termId: Long
)

@Schema(description = "일자별 학습 카운트")
data class DailyProgressPayload(
    @field:PastOrPresent
    @field:Schema(description = "학습 날짜 (YYYY-MM-DD)", example = "2026-05-20")
    val date: LocalDate,
    @field:Min(0)
    @field:Schema(description = "당일 새로 학습한 단어 수", example = "15")
    val learnedCount: Int,
    @field:Min(0)
    @field:Schema(description = "당일 복습한 단어 수", example = "8")
    val reviewedCount: Int
)

@Schema(description = "사용자 전체 학습 요약")
data class UserSummaryPayload(
    @field:Min(0)
    @field:Schema(description = "현재 streak (연속 학습일)", example = "12")
    val currentStreak: Int,
    @field:Min(0)
    @field:Schema(description = "최장 streak", example = "45")
    val longestStreak: Int,
    @field:Min(0)
    @field:Schema(description = "총 학습 단어 수", example = "320")
    val totalLearningTerms: Int
)

data class MigrationStartResponse(
    val migrationId: String
)

data class MigrationStatusResponse(
    val status: String,
    val completedAt: Instant? = null,
    val errorMessage: String? = null
)

data class MigrationUploadResponse(
    val migrationId: String,
    val termProgressCount: Int,
    val glossaryProgressCount: Int,
    val bookmarkCount: Int,
    val dailyProgressCount: Int
)

fun MigrationSessionEntity.toResponse() = MigrationStatusResponse(
    status = status.name,
    completedAt = completedAt,
    errorMessage = errorMessage
)
