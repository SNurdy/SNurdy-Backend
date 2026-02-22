package com.snurdy.backend.domain.admin.changeset.service

import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetDetailResponse
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetItemDetail
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetListItem
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetListResponse
import com.snurdy.backend.domain.admin.changeset.entity.ChangeSetItemStatus
import com.snurdy.backend.domain.admin.changeset.repository.ChangeSetItemRepository
import com.snurdy.backend.domain.admin.changeset.repository.ChangeSetRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminChangeSetQueryService(
    private val changeSetRepository: ChangeSetRepository,
    private val changeSetItemRepository: ChangeSetItemRepository
) {

    @Transactional(readOnly = true)
    fun getChangeSets(): ChangeSetListResponse {
        val items = changeSetRepository.findAllByOrderByCreatedAtDesc().map { changeSet ->
            val id = changeSet.id!!
            ChangeSetListItem(
                id = id,
                title = changeSet.title,
                status = changeSet.status.name,
                sourceEnv = changeSet.sourceEnv.name,
                targetEnv = changeSet.targetEnv.name,
                requestedBy = changeSet.requestedBy.toString(),
                createdAt = changeSet.createdAt?.toString(),
                updatedAt = changeSet.updatedAt?.toString(),
                totalItems = changeSetItemRepository.countByChangeSetId(id),
                validatedItems = changeSetItemRepository.countByChangeSetIdAndStatus(id, ChangeSetItemStatus.VALIDATED),
                failedItems = changeSetItemRepository.countByChangeSetIdAndStatus(id, ChangeSetItemStatus.FAILED)
            )
        }
        return ChangeSetListResponse(changeSets = items)
    }

    @Transactional(readOnly = true)
    fun getChangeSetDetail(changeSetId: Long): ChangeSetDetailResponse {
        val changeSet = changeSetRepository.findById(changeSetId)
            .orElseThrow { ApiException(AdminError.CHANGE_SET_NOT_FOUND) }

        val details = changeSetItemRepository.findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId)
            .map { item ->
                ChangeSetItemDetail(
                    id = item.id!!,
                    entityType = item.entityType,
                    entityPk = item.entityPk,
                    operation = item.operation.name,
                    status = item.status.name,
                    sortOrder = item.sortOrder,
                    errorCode = item.errorCode,
                    errorMessage = item.errorMessage,
                    beforeJson = item.beforeJson,
                    afterJson = item.afterJson,
                    updatedAt = item.updatedAt?.toString()
                )
            }

        return ChangeSetDetailResponse(
            id = changeSet.id!!,
            title = changeSet.title,
            description = changeSet.description,
            status = changeSet.status.name,
            sourceEnv = changeSet.sourceEnv.name,
            targetEnv = changeSet.targetEnv.name,
            requestedBy = changeSet.requestedBy.toString(),
            approvedBy = changeSet.approvedBy?.toString(),
            appliedBy = changeSet.appliedBy?.toString(),
            createdAt = changeSet.createdAt?.toString(),
            updatedAt = changeSet.updatedAt?.toString(),
            approvedAt = changeSet.approvedAt?.toString(),
            appliedAt = changeSet.appliedAt?.toString(),
            items = details,
            validationSummary = changeSet.validationSummary,
            applySummary = changeSet.applySummary
        )
    }
}
