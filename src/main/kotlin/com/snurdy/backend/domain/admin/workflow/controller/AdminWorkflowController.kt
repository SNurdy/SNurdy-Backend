package com.snurdy.backend.domain.admin.workflow.controller

import com.snurdy.backend.domain.admin.workflow.dto.*
import com.snurdy.backend.domain.admin.workflow.service.AdminWorkflowService
import com.snurdy.backend.global.constants.AdminSuccess
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.exception.ApiException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AdminWorkflowController(
    private val adminWorkflowService: AdminWorkflowService
) : AdminWorkflowApi {

    override fun createRequest(request: CreateChangeRequest): ApiResponse<ChangeSetDetailResponse> =
        ApiResponse.success(AdminSuccess.CREATE_CHANGE_REQUEST, adminWorkflowService.createChangeRequest(request, currentUserId()))

    override fun getChangeSets(status: String?, page: Int, size: Int): ApiResponse<ChangeSetPageResponse> =
        ApiResponse.success(CommonSuccess.OK, adminWorkflowService.listChangeSets(status, page, size))

    override fun getChangeSet(changeSetId: Long): ApiResponse<ChangeSetDetailResponse> =
        ApiResponse.success(CommonSuccess.OK, adminWorkflowService.getChangeSet(changeSetId))

    override fun validate(changeSetId: Long): ApiResponse<WorkflowActionResponse> =
        ApiResponse.success(AdminSuccess.VALIDATE_CHANGE_REQUEST, adminWorkflowService.validate(changeSetId, currentUserId()))

    override fun apply(changeSetId: Long): ApiResponse<WorkflowActionResponse> =
        ApiResponse.success(AdminSuccess.APPLY_CHANGE_REQUEST, adminWorkflowService.apply(changeSetId, currentUserId()))

    override fun cancel(changeSetId: Long): ApiResponse<WorkflowActionResponse> =
        ApiResponse.success(AdminSuccess.CANCEL_CHANGE_REQUEST, adminWorkflowService.cancel(changeSetId, currentUserId()))

    private fun currentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw ApiException(CommonError.UNAUTHORIZED)
        val userId = authentication.principal as? UUID
            ?: throw ApiException(CommonError.UNAUTHORIZED)
        return userId
    }
}
