package com.snurdy.backend.global.controller

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "헬스체크 응답")
data class HealthCheckResponse(
    @Schema(description = "서버 상태", example = "UP")
    val status: String,

    @Schema(description = "응답 시각 (UTC, ISO 8601)", example = "2026-05-15T04:45:00.123Z")
    val timestamp: Instant
)
