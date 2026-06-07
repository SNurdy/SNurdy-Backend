package com.snurdy.backend.domain.admin.audit.controller

import com.snurdy.backend.domain.admin.audit.dto.AuditLogPageResponse
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditAction
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Validated
@Tag(name = "Admin AuditLog", description = "관리자 감사 로그 조회 API")
@RequestMapping("/v1/admin/audit-logs")
interface AdminAuditLogApi {

    @Operation(summary = "감사 로그 목록 조회")
    @ApiSuccessCode("ADMIN_S020")
    @GetMapping
    fun list(
        @RequestParam(required = false) action: AdminAuditAction?,
        @RequestParam(required = false) targetTable: String?,
        @RequestParam(required = false) targetPk: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<AuditLogPageResponse>
}