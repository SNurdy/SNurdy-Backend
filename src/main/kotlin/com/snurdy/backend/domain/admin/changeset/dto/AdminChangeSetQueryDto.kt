package com.snurdy.backend.domain.admin.changeset.dto

data class ChangeSetListResponse(
    val changeSets: List<ChangeSetListItem>
)

data class ChangeSetListItem(
    val id: Long,
    val title: String,
    val status: String,
    val sourceEnv: String,
    val targetEnv: String,
    val requestedBy: String,
    val createdAt: String?,
    val updatedAt: String?,
    val totalItems: Long,
    val validatedItems: Long,
    val failedItems: Long
)

data class ChangeSetDetailResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
    val sourceEnv: String,
    val targetEnv: String,
    val requestedBy: String,
    val approvedBy: String?,
    val appliedBy: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val approvedAt: String?,
    val appliedAt: String?,
    val items: List<ChangeSetItemDetail>,
    val validationSummary: String?,
    val applySummary: String?
)

data class ChangeSetItemDetail(
    val id: Long,
    val entityType: String,
    val entityPk: String,
    val operation: String,
    val status: String,
    val sortOrder: Int,
    val errorCode: String?,
    val errorMessage: String?,
    val beforeJson: String?,
    val afterJson: String?,
    val updatedAt: String?
)
