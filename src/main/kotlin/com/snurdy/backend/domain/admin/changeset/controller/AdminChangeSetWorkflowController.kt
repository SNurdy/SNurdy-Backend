package com.snurdy.backend.domain.admin.changeset.controller

import com.snurdy.backend.domain.admin.changeset.model.AdminActor
import com.snurdy.backend.domain.admin.changeset.model.RequestMetadata
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetWorkflowResponse
import com.snurdy.backend.domain.admin.changeset.service.AdminChangeSetWorkflowService
import com.snurdy.backend.global.constants.AdminSuccess
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.exception.ApiException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AdminChangeSetWorkflowController(
    private val workflowService: AdminChangeSetWorkflowService
) : AdminChangeSetWorkflowApi {

    override fun validate(changeSetId: Long): ApiResponse<ChangeSetWorkflowResponse> =
        ApiResponse.success(
            AdminSuccess.VALIDATE,
            workflowService.validate(
                changeSetId = changeSetId,
                actor = currentActor(),
                requestMetadata = requestMetadata()
            )
        )

    override fun approve(changeSetId: Long): ApiResponse<ChangeSetWorkflowResponse> =
        ApiResponse.success(
            AdminSuccess.APPROVE,
            workflowService.approve(
                changeSetId = changeSetId,
                actor = currentActor(),
                requestMetadata = requestMetadata()
            )
        )

    override fun apply(changeSetId: Long): ApiResponse<ChangeSetWorkflowResponse> =
        ApiResponse.success(
            AdminSuccess.APPLY,
            workflowService.apply(
                changeSetId = changeSetId,
                actor = currentActor(),
                requestMetadata = requestMetadata()
            )
        )

    private fun currentActor(): AdminActor {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw ApiException(CommonError.UNAUTHORIZED)

        val userId = authentication.principal as? UUID
            ?: throw ApiException(CommonError.UNAUTHORIZED)

        val roles = authentication.authorities
            .map { it.authority.removePrefix("ROLE_") }
            .toSet()

        return AdminActor(userId = userId, roles = roles)
    }

    private fun requestMetadata(): RequestMetadata {
        val attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
            as? org.springframework.web.context.request.ServletRequestAttributes
            ?: return RequestMetadata()
        val request: HttpServletRequest = attributes.request
        return RequestMetadata(
            requestId = request.getHeader("X-Request-Id"),
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent")
        )
    }
}
