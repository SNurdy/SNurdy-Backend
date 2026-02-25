package com.snurdy.backend.domain.admin.audit.controller

import com.snurdy.backend.domain.admin.audit.dto.AuditLogItem
import com.snurdy.backend.domain.admin.audit.dto.AuditLogPageResponse
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminAuditLogController(
    private val auditLogRepository: AdminAuditLogRepository
) : AdminAuditLogApi {

    override fun list(
        action: String?,
        targetTable: String?,
        targetPk: String?,
        page: Int,
        size: Int
    ): ApiResponse<AuditLogPageResponse> {
        val pageable = PageRequest.of(page, size)

        val result = when {
            !targetTable.isNullOrBlank() && !targetPk.isNullOrBlank() ->
                auditLogRepository.findByTargetTableAndTargetPkOrderByIdDesc(targetTable.trim(), targetPk.trim(), pageable)
            !action.isNullOrBlank() ->
                auditLogRepository.findByActionOrderByIdDesc(action.trim(), pageable)
            else ->
                auditLogRepository.findAllByOrderByIdDesc(pageable)
        }

        return ApiResponse.success(
            CommonSuccess.OK,
            AuditLogPageResponse(
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                items = result.content.map {
                    AuditLogItem(
                        id = it.id!!,
                        actorUserId = it.actorUserId?.toString(),
                        actorRole = it.actorRole,
                        action = it.action,
                        targetTable = it.targetTable,
                        targetPk = it.targetPk,
                        beforeJson = it.beforeJson,
                        afterJson = it.afterJson,
                        ipAddress = it.ipAddress,
                        userAgent = it.userAgent,
                        createdAt = it.createdAt.toString()
                    )
                }
            )
        )
    }
}