package com.snurdy.backend.domain.admin.workflow.dto

import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import org.springframework.data.domain.Page

data class CreateChangeRequest(
    val title: String,
    val description: String? = null,
    val entityType: String,
    val operation: String,
    val termId: Long? = null,
    val morphemeId: Long? = null,
    val spelling: String? = null,
    val meaning: String? = null,
    val abbreviation: String? = null,
    val explanation: String? = null,
    val morphemes: List<Long> = emptyList(),
    val glossaryId: Long? = null,
    val glossaryIds: List<Long>? = null,
)

data class UpdateChangeRequest(
    val title: String? = null,
    val spelling: String? = null,
    val meaning: String? = null,
    val abbreviation: String? = null,
    val explanation: String? = null,
    val morphemes: List<Long>? = null,
    val glossaryId: Long? = null,
    val glossaryIds: List<Long>? = null
)

data class ChangeRequestPageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<ChangeRequestSummary>
)

data class ChangeRequestSummary(
    val id: Long,
    val title: String,
    val status: String,
    val entityType: String,
    val operation: String,
    val entityPk: String,
    val createdAt: String,
    val errorMessage: String?,
    val warningMessage: String?
)

data class ChangeRequestDetailResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
    val entityType: String,
    val operation: String,
    val entityPk: String,
    val beforeJson: String?,
    val afterJson: String?,
    val errorCode: String?,
    val errorMessage: String?,
    val warningMessage: String?,
    val appliedAt: String?,
    val rolledBackAt: String?,
    val createdAt: String
)

data class ChangeRequestActionResponse(
    val id: Long,
    val status: String,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val warningMessage: String? = null
)

fun ChangeRequestEntity.toSummary() = ChangeRequestSummary(
    id = requireId(),
    title = title,
    status = status.name,
    entityType = entityType.name,
    operation = operation.name,
    entityPk = entityPk,
    createdAt = createdAt?.toString() ?: "",
    errorMessage = errorMessage,
    warningMessage = warningMessage
)

fun ChangeRequestEntity.toDetail() = ChangeRequestDetailResponse(
    id = requireId(),
    title = title,
    description = description,
    status = status.name,
    entityType = entityType.name,
    operation = operation.name,
    entityPk = entityPk,
    beforeJson = beforeJson,
    afterJson = afterJson,
    errorCode = errorCode,
    errorMessage = errorMessage,
    warningMessage = warningMessage,
    appliedAt = appliedAt?.toString(),
    rolledBackAt = rolledBackAt?.toString(),
    createdAt = createdAt?.toString() ?: ""
)

fun ChangeRequestEntity.toAction() = ChangeRequestActionResponse(
    id = requireId(),
    status = status.name,
    errorCode = errorCode,
    errorMessage = errorMessage,
    warningMessage = warningMessage
)

fun Page<ChangeRequestEntity>.toChangeRequestPageResponse() = ChangeRequestPageResponse(
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    items = content.map { it.toSummary() }
)
