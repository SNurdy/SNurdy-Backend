package com.snurdy.backend.domain.admin.changeset.dto

data class ChangeSetWorkflowResponse(
    val changeSetId: Long,
    val status: String,
    val totalItems: Int,
    val successItems: Int,
    val failedItems: Int
)
