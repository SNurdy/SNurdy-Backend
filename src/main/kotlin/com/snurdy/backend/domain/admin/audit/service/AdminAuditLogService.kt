package com.snurdy.backend.domain.admin.audit.service

import com.snurdy.backend.domain.admin.audit.dto.AuditLogItem
import com.snurdy.backend.domain.admin.audit.dto.AuditLogPageResponse
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAuditLogService(
    private val auditLogRepository: AdminAuditLogRepository
) {

    @Transactional(readOnly = true)
    fun list(action: String?, targetTable: String?, targetPk: String?, page: Int, size: Int): AuditLogPageResponse {
        val pageable = PageRequest.of(page, size)
        val trimmedTable = targetTable?.trim().takeUnless { it.isNullOrBlank() }
        val trimmedPk = targetPk?.trim().takeUnless { it.isNullOrBlank() }
        val trimmedAction = action?.trim().takeUnless { it.isNullOrBlank() }

        val result = when {
            trimmedTable != null && trimmedPk != null ->
                auditLogRepository.findByTargetTableAndTargetPkOrderByIdDesc(trimmedTable, trimmedPk, pageable)
            trimmedTable != null ->
                auditLogRepository.findByTargetTableOrderByIdDesc(trimmedTable, pageable)
            trimmedAction != null ->
                auditLogRepository.findByActionOrderByIdDesc(trimmedAction, pageable)
            else ->
                auditLogRepository.findAllByOrderByIdDesc(pageable)
        }

        return AuditLogPageResponse(
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            items = result.content.map { it.toDto() }
        )
    }

    private fun AdminAuditLogEntity.toDto() = AuditLogItem(
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
