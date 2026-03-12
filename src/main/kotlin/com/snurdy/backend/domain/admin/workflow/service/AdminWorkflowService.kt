package com.snurdy.backend.domain.admin.workflow.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.domain.admin.workflow.dto.*
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntityType
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.repository.ChangeRequestRepository
import com.snurdy.backend.domain.admin.workflow.service.handler.MorphemeChangeHandler
import com.snurdy.backend.domain.admin.workflow.service.handler.TermChangeHandler
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestBuilder
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestMapper
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminWorkflowService(
    private val changeRequestRepository: ChangeRequestRepository,
    private val termHandler: TermChangeHandler,
    private val morphemeHandler: MorphemeChangeHandler,
    private val builder: ChangeRequestBuilder,
    private val mapper: ChangeRequestMapper,
    private val auditLogRepository: AdminAuditLogRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun create(request: CreateChangeRequest): ChangeRequestDetailResponse {
        val entityType = normalizeEntityType(request.entityType)
        val operation = normalizeOperation(request.operation)
        validateEntityOperation(entityType, operation)
        if (request.title.isBlank()) throw ApiException(CommonError.BAD_REQUEST)

        val entity = builder.build(request, entityType, operation)
        val saved = changeRequestRepository.save(entity)
        audit("REQUEST", "change_requests", saved.id.toString(), null, saved)
        return mapper.toDetail(saved)
    }

    @Transactional(readOnly = true)
    fun list(status: String?, page: Int, size: Int): ChangeRequestPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val result = status?.trim()?.takeIf { it.isNotBlank() }
            ?.let { changeRequestRepository.findByStatusOrderByIdDesc(it, pageable) }
            ?: changeRequestRepository.findAll(pageable)

        return ChangeRequestPageResponse(
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            items = result.content.map { mapper.toSummary(it) }
        )
    }

    @Transactional(readOnly = true)
    fun get(id: Long): ChangeRequestDetailResponse =
        mapper.toDetail(findRequest(id))

    @Transactional
    fun update(id: Long, request: UpdateChangeRequest): ChangeRequestDetailResponse {
        val cr = findRequest(id)
        if (!cr.isEditable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        val currentPayload: MutableMap<String, Any?> = cr.afterJson
            ?.let { objectMapper.readValue(it) }
            ?: mutableMapOf()

        request.title?.trim()?.takeIf { it.isNotBlank() }?.let { cr.title = it }
        request.spelling?.let { currentPayload["spelling"] = it.trim() }
        request.meaning?.let { currentPayload["meaning"] = it.trim() }
        request.abbreviation?.let { currentPayload["abbreviation"] = it.trim().takeIf { v -> v.isNotBlank() } }
        request.explanation?.let { currentPayload["explanation"] = it.trim().takeIf { v -> v.isNotBlank() } }
        request.morphemes?.let { currentPayload["morphemes"] = it }
        request.glossaryIds?.let {
            currentPayload["glossaryIds"] = it
            currentPayload["glossaryId"] = it.firstOrNull()
        } ?: request.glossaryId?.let {
            currentPayload["glossaryId"] = it
            currentPayload["glossaryIds"] = listOf(it)
        }

        cr.afterJson = objectMapper.writeValueAsString(currentPayload)
        cr.resetForRetry()
        changeRequestRepository.save(cr)

        audit("UPDATE", "change_requests", id.toString(), null, cr)
        return mapper.toDetail(cr)
    }

    @Transactional
    fun validate(id: Long): ChangeRequestActionResponse {
        val cr = changeRequestRepository.findByIdForUpdate(id)
            ?: throw ApiException(CommonError.NOT_FOUND)
        if (!cr.isValidatable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        val result = when (cr.entityType) {
            ChangeRequestEntityType.TERM -> termHandler.validate(cr)
            ChangeRequestEntityType.MORPHEME -> morphemeHandler.validate(cr)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        }

        if (result.passed) {
            cr.markValidated(warning = result.warningMessages())
        } else {
            cr.markFailed(
                errorCode = result.errorCodes() ?: AdminError.WORKFLOW_VALIDATION_FAILED.code,
                errorMessage = result.errorMessages() ?: "검증 실패"
            )
        }
        changeRequestRepository.save(cr)
        audit( "VALIDATE", "change_requests", id.toString(), null, cr)

        return mapper.toAction(cr)
    }

    @Transactional
    fun apply(id: Long): ChangeRequestActionResponse {
        val cr = changeRequestRepository.findByIdForUpdate(id)
            ?: throw ApiException(CommonError.NOT_FOUND)
        if (!cr.isApplicable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        val resolvedPk = when (cr.entityType) {
            ChangeRequestEntityType.TERM -> termHandler.apply(cr)
            ChangeRequestEntityType.MORPHEME -> morphemeHandler.apply(cr)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        }

        cr.markApplied(resolvedPk)
        changeRequestRepository.save(cr)
        audit("APPLY", "change_requests", id.toString(), null, cr)

        return mapper.toAction(cr)
    }

    @Transactional
    fun cancel(id: Long): ChangeRequestActionResponse {
        val cr = findRequest(id)
        if (!cr.isCancelable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        cr.markCanceled()
        changeRequestRepository.save(cr)
        audit("CANCEL", "change_requests", id.toString(), null, cr)

        return mapper.toAction(cr)
    }

    @Transactional
    fun rollback(id: Long): ChangeRequestActionResponse {
        val cr = changeRequestRepository.findByIdForUpdate(id)
            ?: throw ApiException(CommonError.NOT_FOUND)
        if (!cr.isRollbackable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        when (cr.entityType) {
            ChangeRequestEntityType.TERM -> termHandler.rollback(cr)
            ChangeRequestEntityType.MORPHEME -> morphemeHandler.rollback(cr)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        }

        cr.markRolledBack()
        changeRequestRepository.save(cr)
        audit("ROLLBACK", "change_requests", id.toString(), null, cr)

        return mapper.toAction(cr)
    }

    private fun findRequest(id: Long): ChangeRequestEntity =
        changeRequestRepository.findById(id).orElseThrow { ApiException(CommonError.NOT_FOUND) }

    private fun normalizeEntityType(raw: String): String {
        val normalized = raw.trim().uppercase()
        if (normalized !in ChangeRequestEntityType.ALL) throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        return normalized
    }

    private fun normalizeOperation(raw: String): String {
        val normalized = raw.trim().uppercase()
        if (normalized !in ChangeRequestOperation.ALL) throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        return normalized
    }

    private fun validateEntityOperation(entityType: String, operation: String) {
        if (entityType == ChangeRequestEntityType.MORPHEME && operation == ChangeRequestOperation.DELETE) {
            throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun audit(action: String, table: String, pk: String, before: Any?, after: Any?) {
        auditLogRepository.save(
            AdminAuditLogEntity(
                actorRole = "ADMIN",
                action = action,
                targetTable = table,
                targetPk = pk,
                beforeJson = before?.let { objectMapper.writeValueAsString(it) },
                afterJson = after?.let { objectMapper.writeValueAsString(it) }
            )
        )
    }
}