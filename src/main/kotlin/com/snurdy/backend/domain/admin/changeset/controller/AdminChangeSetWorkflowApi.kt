package com.snurdy.backend.domain.admin.changeset.controller

import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetWorkflowResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Admin-ChangeSet", description = "관리자 변경 요청 상태 전환 API")
@RequestMapping("/v1/admin/change-sets")
interface AdminChangeSetWorkflowApi {

    @Operation(summary = "변경 요청 검증")
    @PostMapping("/{changeSetId}/validate")
    fun validate(
        @PathVariable changeSetId: Long
    ): ApiResponse<ChangeSetWorkflowResponse>

    @Operation(summary = "변경 요청 승인")
    @PostMapping("/{changeSetId}/approve")
    fun approve(
        @PathVariable changeSetId: Long
    ): ApiResponse<ChangeSetWorkflowResponse>

    @Operation(summary = "변경 요청 적용")
    @PostMapping("/{changeSetId}/apply")
    fun apply(
        @PathVariable changeSetId: Long
    ): ApiResponse<ChangeSetWorkflowResponse>
}
