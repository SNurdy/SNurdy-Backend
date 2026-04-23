package com.snurdy.backend.domain.migration.controller

import com.snurdy.backend.domain.migration.dto.*
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
    @PostMapping("/start")
    fun start(@CurrentUserId userId: UUID): ApiResponse<MigrationStartResponse>

    @Operation(summary = "마이그레이션 거부 처리")
    @PostMapping("/decline")
    fun decline(@CurrentUserId userId: UUID): ApiResponse<MigrationStatusResponse>

    @Operation(summary = "학습 데이터 업로드 — 전체 payload 1회 전송")
    @PostMapping("/{migrationId}/upload")
    fun upload(
        @CurrentUserId userId: UUID,
        @PathVariable migrationId: String,
        @RequestBody @Valid request: MigrationUploadRequest
    ): ApiResponse<MigrationUploadResponse>

    @Operation(summary = "마이그레이션 완료 처리")
    @PostMapping("/{migrationId}/finalize")
    fun finalize(
        @CurrentUserId userId: UUID,
        @PathVariable migrationId: String
    ): ApiResponse<MigrationStatusResponse>

    @Operation(summary = "마이그레이션 상태 조회")
    @GetMapping("/status")
    fun getStatus(@CurrentUserId userId: UUID): ApiResponse<MigrationStatusResponse>
}
