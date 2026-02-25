package com.snurdy.backend.domain.admin.audit.dto

data class AuditLogPageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<AuditLogItem>
)

data class AuditLogItem(
    val id: Long,
    val actorUserId: String?,
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