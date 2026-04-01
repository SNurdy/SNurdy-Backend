package com.snurdy.backend.domain.migration.controller

import com.snurdy.backend.domain.migration.dto.*
import com.snurdy.backend.domain.migration.service.MigrationService
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.MigrationSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.exception.ApiException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MigrationController(
    private val migrationService: MigrationService
) : MigrationApi {
    private fun currentUserId(): UUID =
        (SecurityContextHolder.getContext().authentication?.principal as? UUID)
            ?: throw ApiException(CommonError.UNAUTHORIZED)

    override fun start(): ApiResponse<MigrationStartResponse> =
        ApiResponse.success(MigrationSuccess.START, migrationService.start(currentUserId()))

    override fun decline(): ApiResponse<MigrationStatusResponse> =
        ApiResponse.success(MigrationSuccess.STATUS, migrationService.decline(currentUserId()))

    override fun upload(
        migrationId: String,
        request: MigrationUploadRequest
    ): ApiResponse<MigrationUploadResponse> =
        ApiResponse.success(
            MigrationSuccess.UPLOAD,
            migrationService.upload(
                userId = currentUserId(),
                migrationId = migrationId,
                request = request
            )
        )


    override fun finalize(migrationId: String): ApiResponse<MigrationStatusResponse> =
        ApiResponse.success(MigrationSuccess.FINALIZE, migrationService.finalize(currentUserId(), migrationId))

    override fun getStatus(): ApiResponse<MigrationStatusResponse> =
        ApiResponse.success(MigrationSuccess.STATUS, migrationService.getStatus(currentUserId()))
}
