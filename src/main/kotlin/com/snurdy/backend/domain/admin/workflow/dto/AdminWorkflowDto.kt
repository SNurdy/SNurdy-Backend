package com.snurdy.backend.domain.admin.workflow.dto

data class CreateChangeRequest(
    val title: String,
    val description: String? = null,
    val entityType: String,
    val operation: String,
    val termId: Long? = null,
    val spelling: String? = null,
    val meaning: String? = null,
    val abbreviation: String? = null,
    val explanation: String? = null,
    val morphemes: List<Long> = emptyList(),
    val glossaryId: Long? = null
)

data class ChangeSetPageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<ChangeSetSummary>
)

data class ChangeSetSummary(
    val id: Long,
    val title: String,
    val status: String,
    val requestedBy: String,
    val createdAt: String,
    val validationSummary: String?,
    val applySummary: String?
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
    val validationSummary: String?,
    val applySummary: String?,
    val createdAt: String,
    val items: List<ChangeSetItemDetail>
)

data class ChangeSetItemDetail(
    val id: Long,
    val operation: String,
    val entityType: String,
    val entityPk: String,
    val status: String,
    val beforeJson: String?,
    val afterJson: String?,
    val errorCode: String?,
    val errorMessage: String?
)

data class WorkflowActionResponse(
    val changeSetId: Long,
    val status: String,
    val successCount: Int,
    val failedCount: Int
)
