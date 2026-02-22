package com.snurdy.backend.domain.admin.changeset.controller

import com.snurdy.backend.domain.admin.changeset.model.AdminActor
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetItemCommandResponse
import com.snurdy.backend.domain.admin.changeset.dto.CreateChangeSetRequest
import com.snurdy.backend.domain.admin.changeset.dto.CreateChangeSetResponse
import com.snurdy.backend.domain.admin.changeset.dto.UpsertTermChangeSetItemRequest
import com.snurdy.backend.domain.admin.changeset.service.AdminChangeSetCommandService
import com.snurdy.backend.global.constants.AdminSuccess
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.exception.ApiException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AdminChangeSetCommandController(
    private val commandService: AdminChangeSetCommandService
) : AdminChangeSetCommandApi {

    override fun createChangeSet(request: CreateChangeSetRequest): ApiResponse<CreateChangeSetResponse> =
        ApiResponse.success(AdminSuccess.CREATE_CHANGE_SET, commandService.createChangeSet(request, currentActor()))

    override fun addTermItem(
        changeSetId: Long,
        request: UpsertTermChangeSetItemRequest
    ): ApiResponse<ChangeSetItemCommandResponse> =
        ApiResponse.success(
            AdminSuccess.UPSERT_CHANGE_SET_ITEM,
            commandService.addTermItem(changeSetId, request, currentActor())
        )

    override fun updateTermItem(
        changeSetId: Long,
        itemId: Long,
        request: UpsertTermChangeSetItemRequest
    ): ApiResponse<ChangeSetItemCommandResponse> =
        ApiResponse.success(
            AdminSuccess.UPSERT_CHANGE_SET_ITEM,
            commandService.updateTermItem(changeSetId, itemId, request, currentActor())
        )

    override fun deleteItem(changeSetId: Long, itemId: Long): ApiResponse<Unit> {
        commandService.deleteItem(changeSetId, itemId, currentActor())
        return ApiResponse.success(AdminSuccess.DELETE_CHANGE_SET_ITEM)
    }

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
}
