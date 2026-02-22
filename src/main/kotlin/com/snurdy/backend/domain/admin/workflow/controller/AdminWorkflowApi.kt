package com.snurdy.backend.domain.admin.workflow.controller

import com.snurdy.backend.domain.admin.workflow.dto.ChangeSetDetailResponse
import com.snurdy.backend.domain.admin.workflow.dto.ChangeSetPageResponse
import com.snurdy.backend.domain.admin.workflow.dto.CreateChangeRequest
import com.snurdy.backend.domain.admin.workflow.dto.WorkflowActionResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@Tag(name = "Admin-Workflow", description = "관리자 요청-검증-반영 워크플로우 API")
@RequestMapping("/v1/admin/workflows")
interface AdminWorkflowApi {

    @Operation(summary = "용어 변경 요청 생성")
    @PostMapping("/requests")
    fun createRequest(
        @RequestBody request: CreateChangeRequest
    ): ApiResponse<ChangeSetDetailResponse>

    @Operation(summary = "변경 요청 목록 조회")
    @GetMapping("/change-sets")
    fun getChangeSets(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<ChangeSetPageResponse>

    @Operation(summary = "변경 요청 상세 조회")
    @GetMapping("/change-sets/{changeSetId}")
    fun getChangeSet(
        @PathVariable changeSetId: Long
    ): ApiResponse<ChangeSetDetailResponse>

    @Operation(summary = "변경 요청 검증")
    @PostMapping("/change-sets/{changeSetId}/validate")
    fun validate(
        @PathVariable changeSetId: Long
    ): ApiResponse<WorkflowActionResponse>

    @Operation(summary = "변경 요청 반영")
    @PostMapping("/change-sets/{changeSetId}/apply")
    fun apply(
        @PathVariable changeSetId: Long
    ): ApiResponse<WorkflowActionResponse>

    @Operation(summary = "변경 요청 취소")
    @PostMapping("/change-sets/{changeSetId}/cancel")
    fun cancel(
        @PathVariable changeSetId: Long
    ): ApiResponse<WorkflowActionResponse>
}
