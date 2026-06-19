package com.snurdy.backend.domain.admin.audit.controller

import com.snurdy.backend.domain.admin.audit.dto.AuditLogPageResponse
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditAction
import com.snurdy.backend.domain.admin.audit.service.AdminAuditLogService
import com.snurdy.backend.global.constants.AdminSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminAuditLogController(
    private val auditLogService: AdminAuditLogService
) : AdminAuditLogApi {

    override fun list(
        action: AdminAuditAction?,
        targetTable: String?,
        targetPk: String?,
        page: Int,
        size: Int
    ): ApiResponse<AuditLogPageResponse> =
        ApiResponse.success(AdminSuccess.LIST_AUDIT_LOGS, auditLogService.list(action, targetTable, targetPk, page, size))
}
