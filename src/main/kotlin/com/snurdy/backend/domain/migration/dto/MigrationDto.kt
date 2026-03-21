package com.snurdy.backend.domain.migration.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant

data class MigrationUploadRequest(
    @field:Valid
    @field:Size(max = 5000)
    val termProgresses: List<TermProgressPayload>,
    @field:Valid
    @field:Size(max = 1000)
    val glossaryProgresses: List<GlossaryProgressPayload>
)

data class TermProgressPayload(
    @field:NotBlank
    @field:Size(max = 64)
    val clientGeneratedId: String,
    @field:Positive
    val glossaryId: Long,
    @field:Positive
    val termId: Long,
    @field:NotBlank
    @field:Size(max = 20)
    val status: String,
    @field:DecimalMin("1.0")
    @field:DecimalMax("9.0")  // SM-2 알고리즘 실용 범위
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
    @field:Positive
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
