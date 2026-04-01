package com.snurdy.backend.domain.version.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "버전 체크 응답")
data class VersionCheckResponse(
    @Schema(description = "업데이트 유형", example = "RECOMMEND_UPDATE")
    val updateType: UpdateType,

    @Schema(description = "최신 버전", example = "1.0.2")
    val latestVersion: String,

    @Schema(description = "최소 지원 버전", example = "1.0.0")
    val minimumVersion: String,

    @Schema(description = "권장 업데이트 메시지")
    val updateMessage: String?,

    @Schema(description = "강제 업데이트 메시지")
    val forceUpdateMessage: String?,

    @Schema(description = "스토어 URL")
    val storeUrl: String?
)

enum class UpdateType {
    FORCE_UPDATE,
    RECOMMEND_UPDATE,
    LATEST
}