package com.snurdy.backend.domain.admin.workflow.service.support

import com.snurdy.backend.domain.admin.workflow.dto.ChangeRequestActionResponse
import com.snurdy.backend.domain.admin.workflow.dto.ChangeRequestDetailResponse
import com.snurdy.backend.domain.admin.workflow.dto.ChangeRequestSummary
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import org.springframework.stereotype.Component

@Component
class ChangeRequestMapper {

    fun toSummary(entity: ChangeRequestEntity) = ChangeRequestSummary(
        id = entity.id!!,
        title = entity.title,
        status = entity.status,
        entityType = entity.entityType,
        operation = entity.operation,
        entityPk = entity.entityPk,
        requestedBy = entity.requestedBy.toString(),
        createdAt = entity.createdAt.toString(),
        errorMessage = entity.errorMessage,
        warningMessage = entity.warningMessage
    )

    fun toDetail(entity: ChangeRequestEntity) = ChangeRequestDetailResponse(
        id = entity.id!!,
        title = entity.title,
        description = entity.description,
        status = entity.status,
        entityType = entity.entityType,
        operation = entity.operation,
        entityPk = entity.entityPk,
        beforeJson = entity.beforeJson,
        afterJson = entity.afterJson,
        errorCode = entity.errorCode,
        errorMessage = entity.errorMessage,
        warningMessage = entity.warningMessage,
        requestedBy = entity.requestedBy.toString(),
        appliedBy = entity.appliedBy?.toString(),
        appliedAt = entity.appliedAt?.toString(),
        createdAt = entity.createdAt.toString()
    )

    fun toAction(entity: ChangeRequestEntity) = ChangeRequestActionResponse(
        id = entity.id!!,
        status = entity.status,
        errorCode = entity.errorCode,
        errorMessage = entity.errorMessage,
        warningMessage = entity.warningMessage
    )
}