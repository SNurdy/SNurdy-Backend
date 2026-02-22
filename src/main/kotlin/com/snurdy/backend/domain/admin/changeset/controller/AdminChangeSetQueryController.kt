package com.snurdy.backend.domain.admin.changeset.controller

import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetDetailResponse
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetListResponse
import com.snurdy.backend.domain.admin.changeset.service.AdminChangeSetQueryService
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminChangeSetQueryController(
    private val adminChangeSetQueryService: AdminChangeSetQueryService
) : AdminChangeSetQueryApi {

    override fun getChangeSets(): ApiResponse<ChangeSetListResponse> =
        ApiResponse.success(CommonSuccess.OK, adminChangeSetQueryService.getChangeSets())

    override fun getChangeSetDetail(changeSetId: Long): ApiResponse<ChangeSetDetailResponse> =
        ApiResponse.success(CommonSuccess.OK, adminChangeSetQueryService.getChangeSetDetail(changeSetId))
}
