package com.snurdy.backend.domain.admin.audit.dto

import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import org.springframework.data.domain.Page

data class AuditLogPageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<AuditLogItem>
)

data class AuditLogItem(
    val id: Long,
    val actorRole: String,
    val action: String,
    val targetTable: String,
    val targetPk: String,
    val beforeJson: String?,
    val afterJson: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: String
)

fun AdminAuditLogEntity.toResponse() = AuditLogItem(
    id = id,
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

fun Page<AdminAuditLogEntity>.toResponse() = AuditLogPageResponse(
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    items = content.map { it.toResponse() }
)
