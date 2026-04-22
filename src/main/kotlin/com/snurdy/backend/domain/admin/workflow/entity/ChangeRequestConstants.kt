package com.snurdy.backend.domain.admin.workflow.entity

import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.exception.ApiException

enum class ChangeRequestStatus {
    REQUESTED,
    VALIDATED,
    APPLIED,
    FAILED,
    CANCELED,
    ROLLED_BACK;

    companion object {
        fun fromDb(raw: String): ChangeRequestStatus =
            runCatching { valueOf(raw.trim().uppercase()) }
                .getOrElse { throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION) }

        fun fromRequestOrNull(raw: String?): ChangeRequestStatus? =
            raw?.trim()?.takeIf { it.isNotBlank() }?.let {
                runCatching { valueOf(it.uppercase()) }.getOrNull()
            }
    }
}

enum class ChangeRequestEntityType {
    TERM,
    MORPHEME;

    companion object {
        fun fromRequest(raw: String): ChangeRequestEntityType =
            runCatching { valueOf(raw.trim().uppercase()) }
                .getOrElse { throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE) }

        fun fromDb(raw: String): ChangeRequestEntityType = fromRequest(raw)
    }
}

enum class ChangeRequestOperation {
    CREATE,
    UPDATE,
    DELETE;

    companion object {
        fun fromRequest(raw: String): ChangeRequestOperation =
            runCatching { valueOf(raw.trim().uppercase()) }
                .getOrElse { throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION) }

        fun fromDb(raw: String): ChangeRequestOperation = fromRequest(raw)
    }
}
