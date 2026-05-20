package com.snurdy.backend.domain.migration.controller

import com.snurdy.backend.domain.migration.dto.*
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Migration", description = "로컬 데이터 마이그레이션 API")
@RequestMapping("/v1/migration")
interface MigrationApi {

    @Operation(summary = "마이그레이션 시작 — migrationId 발급")
    @ApiSuccessCode("MIGRATION_S001")
    @ApiErrorCodes(
        "USER_E001",
        "MIGRATION_E001",
        "MIGRATION_E002",
        "MIGRATION_E015",
        "MIGRATION_E018"
    )
    @PostMapping("/start")
    fun start(@CurrentUserId userId: UUID): ApiResponse<MigrationStartResponse>

    @Operation(summary = "마이그레이션 거부 처리")
    @ApiSuccessCode("MIGRATION_S004")
    @ApiErrorCodes("USER_E001")
    @PostMapping("/decline")
    fun decline(@CurrentUserId userId: UUID): ApiResponse<MigrationStatusResponse>

    @Operation(summary = "학습 데이터 업로드 — 전체 payload 1회 전송")
    @ApiSuccessCode("MIGRATION_S002")
    @ApiErrorCodes(
        "USER_E001",
        "MIGRATION_E002",
        "MIGRATION_E003",
        "MIGRATION_E004",
        "MIGRATION_E007",
        "MIGRATION_E008",
        "MIGRATION_E009",
        "MIGRATION_E010",
        "MIGRATION_E012",
        "MIGRATION_E013",
        "MIGRATION_E014",
        "MIGRATION_E015",
        "MIGRATION_E019"
    )
    @PostMapping("/{migrationId}/upload")
    fun upload(
        @CurrentUserId userId: UUID,
        @PathVariable migrationId: String,
        @RequestBody @Valid request: MigrationUploadRequest
    ): ApiResponse<MigrationUploadResponse>

    @Operation(summary = "마이그레이션 완료 처리")
    @ApiSuccessCode("MIGRATION_S003")
    @ApiErrorCodes(
        "USER_E001",
        "MIGRATION_E003",
        "MIGRATION_E005",
        "MIGRATION_E015",
        "MIGRATION_E019"
    )
    @PostMapping("/{migrationId}/finalize")
    fun finalize(
        @CurrentUserId userId: UUID,
        @PathVariable migrationId: String
    ): ApiResponse<MigrationStatusResponse>

    @Operation(summary = "마이그레이션 상태 조회")
    @ApiSuccessCode("MIGRATION_S004")
    @ApiErrorCodes("USER_E001")
    @GetMapping("/status")
    fun getStatus(@CurrentUserId userId: UUID): ApiResponse<MigrationStatusResponse>
}
