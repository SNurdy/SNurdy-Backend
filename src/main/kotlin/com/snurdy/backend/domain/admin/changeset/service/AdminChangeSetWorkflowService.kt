package com.snurdy.backend.domain.admin.changeset.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.domain.admin.changeset.controller.AdminActor
import com.snurdy.backend.domain.admin.changeset.controller.RequestMetadata
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetWorkflowResponse
import com.snurdy.backend.domain.admin.changeset.entity.*
import com.snurdy.backend.domain.admin.changeset.repository.ChangeSetItemRepository
import com.snurdy.backend.domain.admin.changeset.repository.ChangeSetRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AdminChangeSetWorkflowService(
    private val changeSetRepository: ChangeSetRepository,
    private val changeSetItemRepository: ChangeSetItemRepository,
    private val adminAuditLogRepository: AdminAuditLogRepository
) {
    private val objectMapper = jacksonObjectMapper()

    @Transactional
    fun validate(
        changeSetId: Long,
        actor: AdminActor,
        requestMetadata: RequestMetadata
    ): ChangeSetWorkflowResponse {
        requireRole(actor, "ADMIN", "ADMIN_EDITOR", "ADMIN_APPROVER")

        val changeSet = getChangeSet(changeSetId)
        if (changeSet.status !in setOf(ChangeSetStatus.DRAFT, ChangeSetStatus.FAILED)) {
            throw ApiException(AdminError.INVALID_CHANGE_SET_STATUS, "검증 가능한 상태가 아닙니다: ${changeSet.status}")
        }

        val items = changeSetItemRepository.findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId)
        if (items.isEmpty()) {
            throw ApiException(AdminError.MISSING_CHANGE_SET_ITEMS)
        }

        var successCount = 0
        var failedCount = 0

        items.forEach { item ->
            val invalidReason = validateItem(item)
            if (invalidReason == null) {
                item.status = ChangeSetItemStatus.VALIDATED
                item.errorCode = null
                item.errorMessage = null
                successCount++
            } else {
                item.status = ChangeSetItemStatus.FAILED
                item.errorCode = "VALIDATION_ERROR"
                item.errorMessage = invalidReason
                failedCount++
            }
        }
        changeSetItemRepository.saveAll(items)

        changeSet.status = if (failedCount == 0) ChangeSetStatus.VALIDATED else ChangeSetStatus.FAILED
        changeSet.validationSummary = objectMapper.writeValueAsString(
            mapOf(
                "validatedAt" to Instant.now().toString(),
                "totalItems" to items.size,
                "successItems" to successCount,
                "failedItems" to failedCount
            )
        )
        changeSetRepository.save(changeSet)

        audit(
            actor = actor,
            action = "VALIDATE",
            changeSet = changeSet,
            requestMetadata = requestMetadata,
            beforeJson = null,
            afterJson = changeSet.validationSummary
        )

        return ChangeSetWorkflowResponse(
            changeSetId = changeSet.id!!,
            status = changeSet.status.name,
            totalItems = items.size,
            successItems = successCount,
            failedItems = failedCount
        )
    }

    @Transactional
    fun approve(
        changeSetId: Long,
        actor: AdminActor,
        requestMetadata: RequestMetadata
    ): ChangeSetWorkflowResponse {
        requireRole(actor, "ADMIN", "ADMIN_APPROVER")

        val changeSet = getChangeSet(changeSetId)
        if (changeSet.status != ChangeSetStatus.VALIDATED) {
            throw ApiException(AdminError.INVALID_CHANGE_SET_STATUS, "승인 가능한 상태가 아닙니다: ${changeSet.status}")
        }

        val totalItems = changeSetItemRepository.countByChangeSetId(changeSetId).toInt()
        changeSet.status = ChangeSetStatus.APPROVED
        changeSet.approvedBy = actor.userId
        changeSet.approvedAt = Instant.now()
        changeSetRepository.save(changeSet)

        audit(
            actor = actor,
            action = "APPROVE",
            changeSet = changeSet,
            requestMetadata = requestMetadata
        )

        return ChangeSetWorkflowResponse(
            changeSetId = changeSet.id!!,
            status = changeSet.status.name,
            totalItems = totalItems,
            successItems = totalItems,
            failedItems = 0
        )
    }

    @Transactional
    fun apply(
        changeSetId: Long,
        actor: AdminActor,
        requestMetadata: RequestMetadata
    ): ChangeSetWorkflowResponse {
        requireRole(actor, "ADMIN", "ADMIN_APPROVER")

        val changeSet = getChangeSet(changeSetId)
        if (changeSet.status != ChangeSetStatus.APPROVED) {
            throw ApiException(AdminError.INVALID_CHANGE_SET_STATUS, "적용 가능한 상태가 아닙니다: ${changeSet.status}")
        }

        val items = changeSetItemRepository.findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId)
        if (items.isEmpty()) {
            throw ApiException(AdminError.MISSING_CHANGE_SET_ITEMS)
        }

        var appliedCount = 0
        var skippedCount = 0

        items.forEach { item ->
            if (item.status == ChangeSetItemStatus.VALIDATED) {
                item.status = ChangeSetItemStatus.APPLIED
                item.errorCode = null
                item.errorMessage = null
                appliedCount++
            } else {
                item.status = ChangeSetItemStatus.SKIPPED
                skippedCount++
            }
        }
        changeSetItemRepository.saveAll(items)

        changeSet.status = if (changeSet.targetEnv == ChangeSetEnvironment.PROD) {
            ChangeSetStatus.APPLIED_PROD
        } else {
            ChangeSetStatus.APPLIED_DEV
        }
        changeSet.appliedBy = actor.userId
        changeSet.appliedAt = Instant.now()
        changeSet.applySummary = objectMapper.writeValueAsString(
            mapOf(
                "appliedAt" to Instant.now().toString(),
                "totalItems" to items.size,
                "appliedItems" to appliedCount,
                "skippedItems" to skippedCount
            )
        )
        changeSetRepository.save(changeSet)

        audit(
            actor = actor,
            action = "APPLY",
            changeSet = changeSet,
            requestMetadata = requestMetadata,
            beforeJson = null,
            afterJson = changeSet.applySummary
        )

        return ChangeSetWorkflowResponse(
            changeSetId = changeSet.id!!,
            status = changeSet.status.name,
            totalItems = items.size,
            successItems = appliedCount,
            failedItems = skippedCount
        )
    }

    private fun validateItem(item: ChangeSetItemEntity): String? {
        val hasBefore = !item.beforeJson.isNullOrBlank()
        val hasAfter = !item.afterJson.isNullOrBlank()

        return when (item.operation) {
            ChangeSetOperation.CREATE -> if (!hasAfter) "CREATE 항목에는 after_json이 필요합니다" else null
            ChangeSetOperation.UPDATE -> if (!hasAfter) "UPDATE 항목에는 after_json이 필요합니다" else null
            ChangeSetOperation.DELETE -> if (!hasBefore) "DELETE 항목에는 before_json이 필요합니다" else null
            ChangeSetOperation.LINK, ChangeSetOperation.UNLINK -> null
        }
    }

    private fun getChangeSet(changeSetId: Long): ChangeSetEntity =
        changeSetRepository.findById(changeSetId)
            .orElseThrow { ApiException(AdminError.CHANGE_SET_NOT_FOUND) }

    private fun requireRole(actor: AdminActor, vararg allowedRoles: String) {
        if (actor.roles.none { role -> allowedRoles.contains(role) }) {
            throw ApiException(CommonError.FORBIDDEN)
        }
    }

    private fun audit(
        actor: AdminActor,
        action: String,
        changeSet: ChangeSetEntity,
        requestMetadata: RequestMetadata,
        beforeJson: String? = null,
        afterJson: String? = null
    ) {
        adminAuditLogRepository.save(
            AdminAuditLogEntity(
                actorUserId = actor.userId,
                actorRole = actor.roles.sorted().joinToString(","),
                action = action,
                targetTable = "change_sets",
                targetPk = changeSet.id.toString(),
                changeSetId = changeSet.id,
                requestId = requestMetadata.requestId,
                beforeJson = beforeJson,
                afterJson = afterJson,
                ipAddress = requestMetadata.ipAddress,
                userAgent = requestMetadata.userAgent
            )
        )
    }
}
