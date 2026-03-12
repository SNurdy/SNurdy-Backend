package com.snurdy.backend.domain.admin.workflow.controller

import com.snurdy.backend.domain.admin.workflow.dto.*
import com.snurdy.backend.domain.admin.workflow.service.AdminWorkflowService
import com.snurdy.backend.global.constants.AdminSuccess
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminWorkflowController(
    private val adminWorkflowService: AdminWorkflowService
) : AdminWorkflowApi {

    override fun create(request: CreateChangeRequest): ApiResponse<ChangeRequestDetailResponse> =
        ApiResponse.success(AdminSuccess.CREATE_CHANGE_REQUEST, adminWorkflowService.create(request))

    override fun list(status: String?, page: Int, size: Int): ApiResponse<ChangeRequestPageResponse> =
        ApiResponse.success(CommonSuccess.OK, adminWorkflowService.list(status, page, size))

    override fun get(id: Long): ApiResponse<ChangeRequestDetailResponse> =
        ApiResponse.success(CommonSuccess.OK, adminWorkflowService.get(id))

    override fun update(id: Long, request: UpdateChangeRequest): ApiResponse<ChangeRequestDetailResponse> =
        ApiResponse.success(CommonSuccess.OK, adminWorkflowService.update(id, request))

    override fun validate(id: Long): ApiResponse<ChangeRequestActionResponse> =
        ApiResponse.success(AdminSuccess.VALIDATE_CHANGE_REQUEST, adminWorkflowService.validate(id))

    override fun apply(id: Long): ApiResponse<ChangeRequestActionResponse> =
        ApiResponse.success(AdminSuccess.APPLY_CHANGE_REQUEST, adminWorkflowService.apply(id))

    override fun cancel(id: Long): ApiResponse<ChangeRequestActionResponse> =
        ApiResponse.success(AdminSuccess.CANCEL_CHANGE_REQUEST, adminWorkflowService.cancel(id))

    override fun rollback(id: Long): ApiResponse<ChangeRequestActionResponse> =
        ApiResponse.success(AdminSuccess.ROLLBACK_CHANGE_REQUEST, adminWorkflowService.rollback(id))
}