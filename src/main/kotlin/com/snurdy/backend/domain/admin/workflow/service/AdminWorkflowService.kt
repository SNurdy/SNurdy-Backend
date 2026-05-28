// Responsibility: 변경 요청 워크플로우 전체 흐름 오케스트레이션
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
import com.snurdy.backend.domain.admin.workflow.service.handler.ChangeRequestHandler
import com.snurdy.backend.domain.admin.workflow.service.handler.MorphemeChangeHandler
import com.snurdy.backend.domain.admin.workflow.service.handler.TermChangeHandler
import com.snurdy.backend.domain.admin.workflow.service.model.ValidationResult
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestBuilder
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestPayloadMapper
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminWorkflowService(
    private val changeRequestRepository: ChangeRequestRepository,
    private val builder: ChangeRequestBuilder,
    private val auditLogRepository: AdminAuditLogRepository,
    private val payloadMapper: ChangeRequestPayloadMapper,
    termHandler: TermChangeHandler,
    morphemeHandler: MorphemeChangeHandler
) {
    // 엔티티 타입 추가 시 이 맵에만 항목을 추가하면 됨 (OCP)
    private val handlers: Map<ChangeRequestEntityType, ChangeRequestHandler> = mapOf(
        ChangeRequestEntityType.TERM to termHandler,
        ChangeRequestEntityType.MORPHEME to morphemeHandler
    )

    @Transactional
    fun create(request: CreateChangeRequest): ChangeRequestDetailResponse {
        val entityType = ChangeRequestEntityType.fromRequest(request.entityType)
        val operation = ChangeRequestOperation.fromRequest(request.operation)
        if (entityType == ChangeRequestEntityType.MORPHEME && operation == ChangeRequestOperation.DELETE)
            throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        if (request.title.isBlank()) throw ApiException(AdminError.CHANGE_REQUEST_TITLE_REQUIRED)

        val saved = changeRequestRepository.save(builder.build(request, entityType, operation))
        audit(AdminAuditAction.REQUEST, saved.requireId(), null, saved)
        return saved.toDetail()
    }

    @Transactional(readOnly = true)
    fun list(status: String?, page: Int, size: Int): ChangeRequestPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val result = ChangeRequestStatus.fromRequestOrNull(status)
            ?.let { changeRequestRepository.findByStatus(it, pageable) }
            ?: changeRequestRepository.findAll(pageable)
        return result.toChangeRequestPageResponse()
    }

    @Transactional(readOnly = true)
    fun get(id: Long): ChangeRequestDetailResponse = findRequest(id).toDetail()

    @Transactional
    fun update(id: Long, request: UpdateChangeRequest): ChangeRequestDetailResponse {
        val changeRequest = findRequest(id)
        if (!changeRequest.isEditable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        request.title?.trim()?.takeIf { it.isNotBlank() }?.let { changeRequest.title = it }
        changeRequest.afterJson = payloadMapper.writeAsString(patchPayload(changeRequest.afterJson, request))
        changeRequest.resetForRetry()
        changeRequestRepository.save(changeRequest)

        audit(AdminAuditAction.UPDATE, id, null, changeRequest)
        return changeRequest.toDetail()
    }

    @Transactional
    fun validate(id: Long): ChangeRequestActionResponse {
        val changeRequest = findByIdForUpdate(id)
        if (!changeRequest.isValidatable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        val result = handler(changeRequest.entityType).validate(changeRequest)
        applyValidationResult(changeRequest, result)
        changeRequestRepository.save(changeRequest)
        audit(AdminAuditAction.VALIDATE, id, null, changeRequest)
        return changeRequest.toAction()
    }

    @Transactional
    fun apply(id: Long): ChangeRequestActionResponse {
        val changeRequest = findByIdForUpdate(id)
        if (!changeRequest.isApplicable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        val resolvedPk = handler(changeRequest.entityType).apply(changeRequest)
        changeRequest.markApplied(resolvedPk)
        changeRequestRepository.save(changeRequest)
        audit(AdminAuditAction.APPLY, id, null, changeRequest)
        return changeRequest.toAction()
    }

    @Transactional
    fun cancel(id: Long): ChangeRequestActionResponse {
        val changeRequest = findByIdForUpdate(id)
        if (!changeRequest.isCancelable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        changeRequest.markCanceled()
        changeRequestRepository.save(changeRequest)
        audit(AdminAuditAction.CANCEL, id, null, changeRequest)
        return changeRequest.toAction()
    }

    @Transactional
    fun rollback(id: Long): ChangeRequestActionResponse {
        val changeRequest = findByIdForUpdate(id)
        if (!changeRequest.isRollbackable()) throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)

        handler(changeRequest.entityType).rollback(changeRequest)
        changeRequest.markRolledBack()
        changeRequestRepository.save(changeRequest)
        audit(AdminAuditAction.ROLLBACK, id, null, changeRequest)
        return changeRequest.toAction()
    }

    private fun handler(type: ChangeRequestEntityType): ChangeRequestHandler =
        handlers[type] ?: throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)

    private fun findRequest(id: Long): ChangeRequestEntity =
        changeRequestRepository.findById(id).orElseThrow { ApiException(AdminError.CHANGE_REQUEST_NOT_FOUND) }

    private fun findByIdForUpdate(id: Long): ChangeRequestEntity =
        changeRequestRepository.findByIdForUpdate(id) ?: throw ApiException(AdminError.CHANGE_REQUEST_NOT_FOUND)

    private fun applyValidationResult(changeRequest: ChangeRequestEntity, result: ValidationResult) {
        if (result.passed) {
            changeRequest.markValidated(warning = result.warningMessages())
        } else {
            changeRequest.markFailed(
                errorCode = result.errorCodes() ?: AdminError.WORKFLOW_VALIDATION_FAILED.code,
                errorMessage = result.errorMessages() ?: "검증 실패"
            )
        }
    }

    private fun patchPayload(json: String?, request: UpdateChangeRequest): Map<String, Any?> {
        val current = payloadMapper.readMutableMap(json)
        request.spelling?.let { current["spelling"] = it.trim() }
        request.meaning?.let { current["meaning"] = it.trim() }
        request.abbreviation?.let { current["abbreviation"] = it.trim().takeIf { v -> v.isNotBlank() } }
        request.explanation?.let { current["explanation"] = it.trim().takeIf { v -> v.isNotBlank() } }
        request.morphemes?.let { current["morphemes"] = it }
        val glossaryIds = request.glossaryIds ?: request.glossaryId?.let { listOf(it) }
        glossaryIds?.let { current["glossaryIds"] = it }
        return current
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

    companion object {
        private const val ACTOR_ROLE = "ADMIN"
    }
}
