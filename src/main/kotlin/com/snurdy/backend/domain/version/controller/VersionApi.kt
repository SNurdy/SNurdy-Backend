package com.snurdy.backend.domain.version.controller

import com.snurdy.backend.domain.version.dto.VersionCheckResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Version", description = "앱 버전 체크 API")
@RequestMapping("/v1/app/version")
interface VersionApi {

    @Operation(summary = "앱 버전 체크")
    @SecurityRequirements
    @GetMapping("/check")
    fun checkVersion(
        @Parameter(description = "클라이언트 설치 버전", example = "1.0.0", required = true)
        @RequestParam clientVersion: String,
        @Parameter(description = "플랫폼", example = "IOS", required = true)
        @RequestParam platform: String
    ): ApiResponse<VersionCheckResponse>
}