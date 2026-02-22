package com.snurdy.backend.domain.admin.changeset.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.domain.admin.changeset.model.AdminActor
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetItemCommandResponse
import com.snurdy.backend.domain.admin.changeset.dto.CreateChangeSetRequest
import com.snurdy.backend.domain.admin.changeset.dto.CreateChangeSetResponse
import com.snurdy.backend.domain.admin.changeset.dto.UpsertTermChangeSetItemRequest
import com.snurdy.backend.domain.admin.changeset.entity.*
import com.snurdy.backend.domain.admin.changeset.repository.ChangeSetItemRepository
import com.snurdy.backend.domain.admin.changeset.repository.ChangeSetRepository
import com.snurdy.backend.domain.catalog.term.repository.TermRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.CatalogError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminChangeSetCommandService(
    private val changeSetRepository: ChangeSetRepository,
    private val changeSetItemRepository: ChangeSetItemRepository,
    private val termRepository: TermRepository,
    private val adminAuditLogRepository: AdminAuditLogRepository
) {
    private val objectMapper = jacksonObjectMapper()

    @Transactional
    fun createChangeSet(request: CreateChangeSetRequest, actor: AdminActor): CreateChangeSetResponse {
        requireRole(actor, "ADMIN", "ADMIN_EDITOR", "ADMIN_APPROVER")

        val entity = ChangeSetEntity(
            title = request.title.trim(),
            description = request.description?.trim(),
            status = ChangeSetStatus.DRAFT,
            sourceEnv = parseEnv(request.sourceEnv),
            targetEnv = parseEnv(request.targetEnv),
            requestedBy = actor.userId
        )
        val saved = changeSetRepository.save(entity)
        audit(actor, "CREATE", "change_sets", saved.id.toString(), saved.id, null, null)

        return CreateChangeSetResponse(changeSetId = saved.id!!, status = saved.status.name)
    }

    @Transactional
    fun addTermItem(
        changeSetId: Long,
        request: UpsertTermChangeSetItemRequest,
        actor: AdminActor
    ): ChangeSetItemCommandResponse {
        requireRole(actor, "ADMIN", "ADMIN_EDITOR")
        val changeSet = getMutableChangeSet(changeSetId)

        val operation = parseOperation(request.operation)
        val beforeJson = buildBeforeJson(operation, request.termId)
        val afterJson = buildAfterJson(operation, request)
        val sortOrder = request.sortOrder ?: ((changeSetItemRepository
            .findTopByChangeSetIdOrderBySortOrderDescIdDesc(changeSetId)?.sortOrder ?: 0) + 1)

        val saved = changeSetItemRepository.save(
            ChangeSetItemEntity(
                changeSetId = changeSet.id!!,
                entityType = "TERM",
                entityPk = request.termId?.toString() ?: "NEW-${System.currentTimeMillis()}",
                operation = operation,
                beforeJson = beforeJson,
                afterJson = afterJson,
                status = ChangeSetItemStatus.PENDING,
                sortOrder = sortOrder
            )
        )
        audit(actor, "CREATE", "change_set_items", saved.id.toString(), changeSet.id, null, saved.afterJson)
        return ChangeSetItemCommandResponse(changeSet.id!!, saved.id!!, saved.operation.name, saved.status.name)
    }

    @Transactional
    fun updateTermItem(
        changeSetId: Long,
        itemId: Long,
        request: UpsertTermChangeSetItemRequest,
        actor: AdminActor
    ): ChangeSetItemCommandResponse {
        requireRole(actor, "ADMIN", "ADMIN_EDITOR")
        val changeSet = getMutableChangeSet(changeSetId)
        val item = changeSetItemRepository.findByIdAndChangeSetId(itemId, changeSetId)
            ?: throw ApiException(AdminError.CHANGE_SET_ITEM_NOT_FOUND)

        val before = item.afterJson

        val operation = parseOperation(request.operation)
        item.operation = operation
        item.entityType = "TERM"
        item.entityPk = request.termId?.toString() ?: item.entityPk
        item.beforeJson = buildBeforeJson(operation, request.termId)
        item.afterJson = buildAfterJson(operation, request)
        item.status = ChangeSetItemStatus.PENDING
        item.errorCode = null
        item.errorMessage = null
        request.sortOrder?.let { item.sortOrder = it }

        val saved = changeSetItemRepository.save(item)
        audit(actor, "UPDATE", "change_set_items", saved.id.toString(), changeSet.id, before, saved.afterJson)
        return ChangeSetItemCommandResponse(changeSet.id!!, saved.id!!, saved.operation.name, saved.status.name)
    }

    @Transactional
    fun deleteItem(changeSetId: Long, itemId: Long, actor: AdminActor) {
        requireRole(actor, "ADMIN", "ADMIN_EDITOR")
        val changeSet = getMutableChangeSet(changeSetId)
        val item = changeSetItemRepository.findByIdAndChangeSetId(itemId, changeSetId)
            ?: throw ApiException(AdminError.CHANGE_SET_ITEM_NOT_FOUND)
        val before = item.afterJson
        changeSetItemRepository.delete(item)
        audit(actor, "DELETE", "change_set_items", itemId.toString(), changeSet.id, before, null)
    }

    private fun buildBeforeJson(operation: ChangeSetOperation, termId: Long?): String? {
        if (operation == ChangeSetOperation.CREATE) return null
        val id = termId ?: throw ApiException(AdminError.TERM_ID_REQUIRED)
        val term = termRepository.findById(id).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
        return objectMapper.writeValueAsString(
            mapOf(
                "id" to term.id,
                "spelling" to term.spelling,
                "meaning" to term.meaning,
                "abbreviation" to term.abbreviation,
                "explanation" to term.explanation
            )
        )
    }

    private fun buildAfterJson(operation: ChangeSetOperation, request: UpsertTermChangeSetItemRequest): String? {
        if (operation == ChangeSetOperation.DELETE) return null

        if (request.spelling.isNullOrBlank() || request.meaning.isNullOrBlank()) {
            throw ApiException(AdminError.TERM_REQUIRED_FIELDS_MISSING)
        }

        val payload = linkedMapOf<String, Any?>(
            "spelling" to request.spelling.trim(),
            "meaning" to request.meaning.trim(),
            "abbreviation" to request.abbreviation?.trim(),
            "explanation" to request.explanation?.trim()
        )
        request.termId?.let { payload["id"] = it }

        return objectMapper.writeValueAsString(payload)
    }

    private fun parseEnv(raw: String): ChangeSetEnvironment = runCatching {
        ChangeSetEnvironment.valueOf(raw.trim().uppercase())
    }.getOrElse {
        throw ApiException(AdminError.INVALID_CHANGE_SET_ENV)
    }

    private fun parseOperation(raw: String): ChangeSetOperation {
        val op = runCatching { ChangeSetOperation.valueOf(raw.trim().uppercase()) }
            .getOrElse {
                throw ApiException(AdminError.INVALID_CHANGE_SET_OPERATION)
            }
        if (op !in setOf(ChangeSetOperation.CREATE, ChangeSetOperation.UPDATE, ChangeSetOperation.DELETE)) {
            throw ApiException(AdminError.UNSUPPORTED_TERM_OPERATION)
        }
        return op
    }

    private fun getMutableChangeSet(changeSetId: Long): ChangeSetEntity {
        val changeSet = changeSetRepository.findById(changeSetId)
            .orElseThrow { ApiException(AdminError.CHANGE_SET_NOT_FOUND) }
        if (changeSet.status !in setOf(ChangeSetStatus.DRAFT, ChangeSetStatus.FAILED)) {
            throw ApiException(AdminError.IMMUTABLE_CHANGE_SET)
        }
        return changeSet
    }

    private fun requireRole(actor: AdminActor, vararg allowedRoles: String) {
        if (actor.roles.none { role -> allowedRoles.contains(role) }) {
            throw ApiException(CommonError.FORBIDDEN)
        }
    }

    private fun audit(
        actor: AdminActor,
        action: String,
        targetTable: String,
        targetPk: String,
        changeSetId: Long?,
        beforeJson: String?,
        afterJson: String?
    ) {
        adminAuditLogRepository.save(
            AdminAuditLogEntity(
                actorUserId = actor.userId,
                actorRole = actor.roles.sorted().joinToString(","),
                action = action,
                targetTable = targetTable,
                targetPk = targetPk,
                changeSetId = changeSetId,
                beforeJson = beforeJson,
                afterJson = afterJson
            )
        )
    }
}
