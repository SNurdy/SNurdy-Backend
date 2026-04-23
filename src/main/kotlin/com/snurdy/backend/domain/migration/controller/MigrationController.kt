package com.snurdy.backend.domain.migration.controller

import com.snurdy.backend.domain.migration.dto.*
import com.snurdy.backend.domain.migration.service.MigrationService
import com.snurdy.backend.global.constants.MigrationSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MigrationController(
    private val migrationService: MigrationService
) : MigrationApi {

    override fun start(userId: UUID): ApiResponse<MigrationStartResponse> =
        ApiResponse.success(MigrationSuccess.START, migrationService.start(userId))

    override fun decline(userId: UUID): ApiResponse<MigrationStatusResponse> =
        ApiResponse.success(MigrationSuccess.STATUS, migrationService.decline(userId))

    override fun upload(
        userId: UUID,
        migrationId: String,
        request: MigrationUploadRequest
    ): ApiResponse<MigrationUploadResponse> =
        ApiResponse.success(
            MigrationSuccess.UPLOAD,
            migrationService.upload(userId = userId, migrationId = migrationId, request = request)
        )

    override fun finalize(userId: UUID, migrationId: String): ApiResponse<MigrationStatusResponse> =
        ApiResponse.success(MigrationSuccess.FINALIZE, migrationService.finalize(userId, migrationId))

    override fun getStatus(userId: UUID): ApiResponse<MigrationStatusResponse> =
        ApiResponse.success(MigrationSuccess.STATUS, migrationService.getStatus(userId))
}
