package com.snurdy.backend.domain.admin.workflow.service

import com.snurdy.backend.domain.admin.audit.entity.AdminAuditAction
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditTarget
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.domain.admin.workflow.dto.*
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntityType
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestStatus
import com.snurdy.backend.domain.admin.workflow.repository.ChangeRequestRepository
import com.snurdy.backend.domain.admin.workflow.service.handler.MorphemeChangeHandler
import com.snurdy.backend.domain.admin.workflow.service.handler.TermChangeHandler
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestBuilder
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestMapper
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestPayloadMapper
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
    private val payloadMapper: ChangeRequestPayloadMapper
) {

    @Transactional
    fun create(request: CreateChangeRequest): ChangeRequestDetailResponse {
        val entityType = ChangeRequestEntityType.fromRequest(request.entityType)
        val operation = ChangeRequestOperation.fromRequest(request.operation)
        validateEntityOperation(entityType, operation)
        if (request.title.isBlank()) throw ApiException(CommonError.BAD_REQUEST)

        val entity = builder.build(request, entityType, operation)
        val saved = changeRequestRepository.save(entity)
        audit(AdminAuditAction.REQUEST, saved.requireId(), null, saved)
        return mapper.toDetail(saved)
    }

    @Transactional(readOnly = true)
    fun list(status: String?, page: Int, size: Int): ChangeRequestPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val parsedStatus = ChangeRequestStatus.fromRequestOrNull(status)
        val result = parsedStatus
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
        val changeRequest = findRequest(id)
        if (!changeRequest.isEditable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        val currentPayload = payloadMapper.readMutableMap(changeRequest.afterJson)

        request.title?.trim()?.takeIf { it.isNotBlank() }?.let { changeRequest.title = it }
        request.spelling?.let { currentPayload["spelling"] = it.trim() }
        request.meaning?.let { currentPayload["meaning"] = it.trim() }
        request.abbreviation?.let { currentPayload["abbreviation"] = it.trim().takeIf { v -> v.isNotBlank() } }
        request.explanation?.let { currentPayload["explanation"] = it.trim().takeIf { v -> v.isNotBlank() } }
        request.morphemes?.let { currentPayload["morphemes"] = it }
        request.glossaryIds?.let {
            currentPayload["glossaryIds"] = it
        } ?: request.glossaryId?.let {
            currentPayload["glossaryIds"] = listOf(it)
        }

        changeRequest.afterJson = payloadMapper.writeAsString(currentPayload)
        changeRequest.resetForRetry()
        changeRequestRepository.save(changeRequest)

        audit(AdminAuditAction.UPDATE, id, null, changeRequest)
        return mapper.toDetail(changeRequest)
    }

    @Transactional
    fun validate(id: Long): ChangeRequestActionResponse {
        val changeRequest = changeRequestRepository.findByIdForUpdate(id)
            ?: throw ApiException(CommonError.NOT_FOUND)
        if (!changeRequest.isValidatable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        val result = when (changeRequest.entityType) {
            ChangeRequestEntityType.TERM -> termHandler.validate(changeRequest)
            ChangeRequestEntityType.MORPHEME -> morphemeHandler.validate(changeRequest)
        }

        if (result.passed) {
            changeRequest.markValidated(warning = result.warningMessages())
        } else {
            changeRequest.markFailed(
                errorCode = result.errorCodes() ?: AdminError.WORKFLOW_VALIDATION_FAILED.code,
                errorMessage = result.errorMessages() ?: "검증 실패"
            )
        }
        changeRequestRepository.save(changeRequest)
        audit(AdminAuditAction.VALIDATE, id, null, changeRequest)

        return mapper.toAction(changeRequest)
    }

    @Transactional
    fun apply(id: Long): ChangeRequestActionResponse {
        val changeRequest = changeRequestRepository.findByIdForUpdate(id)
            ?: throw ApiException(CommonError.NOT_FOUND)
        if (!changeRequest.isApplicable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        val resolvedPk = when (changeRequest.entityType) {
            ChangeRequestEntityType.TERM -> termHandler.apply(changeRequest)
            ChangeRequestEntityType.MORPHEME -> morphemeHandler.apply(changeRequest)
        }

        changeRequest.markApplied(resolvedPk)
        changeRequestRepository.save(changeRequest)
        audit(AdminAuditAction.APPLY, id, null, changeRequest)

        return mapper.toAction(changeRequest)
    }

    @Transactional
    fun cancel(id: Long): ChangeRequestActionResponse {
        val changeRequest = changeRequestRepository.findByIdForUpdate(id)
            ?: throw ApiException(CommonError.NOT_FOUND)
        if (!changeRequest.isCancelable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        changeRequest.markCanceled()
        changeRequestRepository.save(changeRequest)
        audit(AdminAuditAction.CANCEL, id, null, changeRequest)

        return mapper.toAction(changeRequest)
    }

    @Transactional
    fun rollback(id: Long): ChangeRequestActionResponse {
        val changeRequest = changeRequestRepository.findByIdForUpdate(id)
            ?: throw ApiException(CommonError.NOT_FOUND)
        if (!changeRequest.isRollbackable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        when (changeRequest.entityType) {
            ChangeRequestEntityType.TERM -> termHandler.rollback(changeRequest)
            ChangeRequestEntityType.MORPHEME -> morphemeHandler.rollback(changeRequest)
        }

        changeRequest.markRolledBack()
        changeRequestRepository.save(changeRequest)
        audit(AdminAuditAction.ROLLBACK, id, null, changeRequest)

        return mapper.toAction(changeRequest)
    }

    private fun findRequest(id: Long): ChangeRequestEntity =
        changeRequestRepository.findById(id).orElseThrow { ApiException(CommonError.NOT_FOUND) }

    private fun validateEntityOperation(
        entityType: ChangeRequestEntityType,
        operation: ChangeRequestOperation
    ) {
        if (entityType == ChangeRequestEntityType.MORPHEME && operation == ChangeRequestOperation.DELETE) {
            throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun audit(action: AdminAuditAction, pk: Long, before: Any?, after: Any?) {
        auditLogRepository.save(
            AdminAuditLogEntity(
                actorRole = ACTOR_ROLE,
                action = action.name,
                targetTable = AdminAuditTarget.CHANGE_REQUESTS,
                targetPk = pk.toString(),
                beforeJson = payloadMapper.writeAsString(before),
                afterJson = payloadMapper.writeAsString(after)
            )
        )
    }

    private fun ChangeRequestEntity.requireId(): Long =
        id ?: throw ApiException(CommonError.INTERNAL_ERROR)

    companion object {
        private const val ACTOR_ROLE = "ADMIN"
    }
}
