package com.snurdy.backend.domain.migration.dto

import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class MigrationUploadRequest(
    @field:Valid
    @field:Size(max = 5000)
    val termProgresses: List<TermProgressPayload>,
    @field:Valid
    @field:Size(max = 1000)
    val glossaryProgresses: List<GlossaryProgressPayload>,
    @field:Valid
    @field:Size(max = 5000)
    val bookmarks: List<BookmarkPayload> = emptyList(),
    @field:Valid
    @field:Size(max = 3650)
    val dailyProgresses: List<DailyProgressPayload> = emptyList(),
    @field:Valid
    val userSummary: UserSummaryPayload? = null
)

data class TermProgressPayload(
    @field:Positive
    val glossaryId: Long,
    @field:Positive
    val termId: Long,
    val status: TermProgressStatus,
    @field:DecimalMin("1.0")
    @field:DecimalMax("9.0")  // SM-2 알고리즘 실용 범위
    val easeFactor: Double,
    @field:Min(0)
    val intervalDays: Int,
    @field:Min(0)
    val reviewCount: Int,
    val lastReviewedAt: Instant?,
    val nextReviewAt: Instant?
)

data class GlossaryProgressPayload(
    @field:Positive
    val glossaryId: Long,
    @field:Min(0)
    val learnedCount: Int,
    val lastLearnedAt: Instant?
)

data class BookmarkPayload(
    @field:Positive
    val glossaryId: Long,
    @field:Positive
    val termId: Long
)

data class DailyProgressPayload(
    val date: LocalDate,
    @field:Min(0)
    val learnedCount: Int,
    @field:Min(0)
    val reviewedCount: Int
)

data class UserSummaryPayload(
    @field:Min(0)
    val currentStreak: Int,
    @field:Min(0)
    val longestStreak: Int,
    @field:Min(0)
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
