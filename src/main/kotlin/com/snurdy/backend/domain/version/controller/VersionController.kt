package com.snurdy.backend.domain.version.controller

import com.snurdy.backend.domain.version.dto.VersionCheckResponse
import com.snurdy.backend.domain.version.service.VersionService
import com.snurdy.backend.global.constants.VersionSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class VersionController(
    private val versionService: VersionService
) : VersionApi {

    override fun checkVersion(clientVersion: String, platform: String): ApiResponse<VersionCheckResponse> =
        ApiResponse.success(VersionSuccess.CHECK, versionService.checkVersion(clientVersion, platform))
}