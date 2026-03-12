package com.snurdy.backend.domain.migration.dto

import java.time.Instant

data class MigrationUploadRequest(
    val termProgresses: List<TermProgressPayload>,
    val glossaryProgresses: List<GlossaryProgressPayload>
)

data class TermProgressPayload(
    val clientGeneratedId: String,
    val glossaryId: Long,
    val termId: Long,
    val status: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val reviewCount: Int,
    val lastReviewedAt: Instant?,
    val nextReviewAt: Instant?,
    val occurredAt: Instant?
)

data class GlossaryProgressPayload(
    val glossaryId: Long,
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