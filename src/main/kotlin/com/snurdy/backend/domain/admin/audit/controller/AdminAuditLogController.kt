package com.snurdy.backend.domain.admin.audit.controller

import com.snurdy.backend.domain.admin.audit.dto.AuditLogItem
import com.snurdy.backend.domain.admin.audit.dto.AuditLogPageResponse
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.exception.ApiException
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
                items = result.content.map { it.toDto() }
            )
        )
    }

    private fun AdminAuditLogEntity.toDto(): AuditLogItem = AuditLogItem(
        id = id ?: throw ApiException(CommonError.INTERNAL_ERROR),
        actorRole = actorRole,
        action = action,
        targetTable = targetTable,
        targetPk = targetPk,
        beforeJson = beforeJson,
        afterJson = afterJson,
        ipAddress = ipAddress,
        userAgent = userAgent,
        createdAt = createdAt.toString()
    )
}
