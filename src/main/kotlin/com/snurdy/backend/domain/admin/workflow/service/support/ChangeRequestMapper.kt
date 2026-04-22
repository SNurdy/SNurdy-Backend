package com.snurdy.backend.domain.admin.workflow.service.support

import com.snurdy.backend.domain.admin.workflow.dto.ChangeRequestActionResponse
import com.snurdy.backend.domain.admin.workflow.dto.ChangeRequestDetailResponse
import com.snurdy.backend.domain.admin.workflow.dto.ChangeRequestSummary
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

@Component
class ChangeRequestMapper {

    fun toSummary(entity: ChangeRequestEntity) = ChangeRequestSummary(
        id = entity.requireId(),
        title = entity.title,
        status = entity.status.name,
        entityType = entity.entityType.name,
        operation = entity.operation.name,
        entityPk = entity.entityPk,
        createdAt = entity.createdAt?.toString() ?: "",
        errorMessage = entity.errorMessage,
        warningMessage = entity.warningMessage
    )

    fun toDetail(entity: ChangeRequestEntity) = ChangeRequestDetailResponse(
        id = entity.requireId(),
        title = entity.title,
        description = entity.description,
        status = entity.status.name,
        entityType = entity.entityType.name,
        operation = entity.operation.name,
        entityPk = entity.entityPk,
        beforeJson = entity.beforeJson,
        afterJson = entity.afterJson,
        errorCode = entity.errorCode,
        errorMessage = entity.errorMessage,
        warningMessage = entity.warningMessage,
        appliedAt = entity.appliedAt?.toString(),
        rolledBackAt = entity.rolledBackAt?.toString(),
        createdAt = entity.createdAt?.toString() ?: ""
    )

    fun toAction(entity: ChangeRequestEntity) = ChangeRequestActionResponse(
        id = entity.requireId(),
        status = entity.status.name,
        errorCode = entity.errorCode,
        errorMessage = entity.errorMessage,
        warningMessage = entity.warningMessage
    )

    private fun ChangeRequestEntity.requireId(): Long =
        id ?: throw ApiException(CommonError.INTERNAL_ERROR)
}
