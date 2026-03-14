package com.snurdy.backend.domain.migration.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class MigrationUploadRequest(
    @field:Valid
    val termProgresses: List<TermProgressPayload>,
    @field:Valid
    val glossaryProgresses: List<GlossaryProgressPayload>
)

data class TermProgressPayload(
    @field:NotBlank
    val clientGeneratedId: String,
    val glossaryId: Long,
    val termId: Long,
    @field:NotBlank
    val status: String,
    @field:Min(1)
    @field:Max(9)  // SM-2 알고리즘 실용 범위
    val easeFactor: Double,
    @field:Min(0)
    val intervalDays: Int,
    @field:Min(0)
    val reviewCount: Int,
    val lastReviewedAt: Instant?,
    val nextReviewAt: Instant?,
    val occurredAt: Instant?
)

data class GlossaryProgressPayload(
    val glossaryId: Long,
    @field:Min(0)
    val learnedCount: Int,
    val lastLearnedAt: Instant?
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
    val glossaryProgressCount: Int
)