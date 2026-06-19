package com.snurdy.backend.domain.admin.audit.service

import com.snurdy.backend.domain.admin.audit.dto.AuditLogPageResponse
import com.snurdy.backend.domain.admin.audit.dto.toResponse
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditAction
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAuditLogService(
    private val auditLogRepository: AdminAuditLogRepository
) {

    @Transactional(readOnly = true)
    fun list(
        action: AdminAuditAction?,
        targetTable: String?,
        targetPk: String?,
        page: Int,
        size: Int
    ): AuditLogPageResponse {
        val pageable = PageRequest.of(page, size)
        val trimmedTable = targetTable?.trim().takeUnless { it.isNullOrBlank() }
        val trimmedPk = targetPk?.trim().takeUnless { it.isNullOrBlank() }

        val result = when {
            trimmedTable != null && trimmedPk != null ->
                auditLogRepository.findByTargetTableAndTargetPkOrderByIdDesc(trimmedTable, trimmedPk, pageable)
            trimmedTable != null ->
                auditLogRepository.findByTargetTableOrderByIdDesc(trimmedTable, pageable)
            action != null ->
                auditLogRepository.findByActionOrderByIdDesc(action.name, pageable)
            else ->
                auditLogRepository.findAllByOrderByIdDesc(pageable)
        }

        return result.toResponse()
    }

    // 호출자의 트랜잭션과 분리된 별도 트랜잭션으로 audit log를 commit한다.
    // 호출자가 롤백되어도 보안 로그는 남는다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun write(
        actorRole: String,
        action: AdminAuditAction,
        targetTable: String,
        targetPk: String,
        changeSetId: Long? = null,
        requestId: String? = null,
        beforeJson: String? = null,
        afterJson: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ) {
        auditLogRepository.save(
            AdminAuditLogEntity(
                actorRole = actorRole,
                action = action.name,
                targetTable = targetTable,
                targetPk = targetPk.take(MAX_PK_LENGTH),
                changeSetId = changeSetId,
                requestId = requestId?.take(MAX_REQUEST_ID_LENGTH),
                beforeJson = beforeJson,
                afterJson = afterJson,
                ipAddress = ipAddress?.take(MAX_IP_LENGTH),
                userAgent = userAgent?.take(MAX_UA_LENGTH)
            )
        )
    }

    companion object {
        private const val MAX_PK_LENGTH = 100
        private const val MAX_IP_LENGTH = 45
        private const val MAX_UA_LENGTH = 255
        private const val MAX_REQUEST_ID_LENGTH = 64
    }
}
