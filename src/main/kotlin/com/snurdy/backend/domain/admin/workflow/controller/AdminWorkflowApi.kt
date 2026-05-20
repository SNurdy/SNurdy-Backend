package com.snurdy.backend.domain.admin.workflow.controller

import com.snurdy.backend.domain.admin.workflow.dto.*
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@Tag(name = "Admin-Workflow", description = "관리자 변경 요청 워크플로우 API")
@RequestMapping("/v1/admin/workflows")
interface AdminWorkflowApi {

    @Operation(summary = "변경 요청 생성")
    @ApiSuccessCode("ADMIN_S010")
    @ApiErrorCodes(
        "ADMIN_E013",
        "ADMIN_E014",
        "ADMIN_E020",
        "ADMIN_E027",
        "ADMIN_E028",
        "ADMIN_E031"
    )
    @PostMapping("/change-requests")
    fun create(@RequestBody request: CreateChangeRequest): ApiResponse<ChangeRequestDetailResponse>

    @Operation(summary = "변경 요청 목록 조회")
    @ApiSuccessCode("ADMIN_S015")
    @GetMapping("/change-requests")
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<ChangeRequestPageResponse>

    @Operation(summary = "변경 요청 상세 조회")
    @ApiSuccessCode("ADMIN_S016")
    @ApiErrorCodes("ADMIN_E029")
    @GetMapping("/change-requests/{id}")
    fun get(@PathVariable id: Long): ApiResponse<ChangeRequestDetailResponse>

    @Operation(summary = "변경 요청 수정 (FAILED/REQUESTED 상태만)")
    @ApiSuccessCode("ADMIN_S017")
    @ApiErrorCodes("ADMIN_E015", "ADMIN_E029", "ADMIN_E031")
    @PutMapping("/change-requests/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: UpdateChangeRequest): ApiResponse<ChangeRequestDetailResponse>

    @Operation(summary = "검증")
    @ApiSuccessCode("ADMIN_S011")
    @ApiErrorCodes("ADMIN_E015", "ADMIN_E016", "ADMIN_E025", "ADMIN_E029", "ADMIN_E031")
    @PostMapping("/change-requests/{id}/validate")
    fun validate(@PathVariable id: Long): ApiResponse<ChangeRequestActionResponse>

    @Operation(summary = "반영 (dev DB)")
    @ApiSuccessCode("ADMIN_S012")
    @ApiErrorCodes(
        "ADMIN_E013",
        "ADMIN_E015",
        "ADMIN_E020",
        "ADMIN_E025",
        "ADMIN_E029",
        "DICTIONARY_E001",
        "DICTIONARY_E002"
    )
    @PostMapping("/change-requests/{id}/apply")
    fun apply(@PathVariable id: Long): ApiResponse<ChangeRequestActionResponse>

    @Operation(summary = "취소")
    @ApiSuccessCode("ADMIN_S013")
    @ApiErrorCodes("ADMIN_E015", "ADMIN_E029")
    @PostMapping("/change-requests/{id}/cancel")
    fun cancel(@PathVariable id: Long): ApiResponse<ChangeRequestActionResponse>

    @Operation(summary = "롤백 (APPLIED 상태만)")
    @ApiSuccessCode("ADMIN_S014")
    @ApiErrorCodes(
        "ADMIN_E013",
        "ADMIN_E015",
        "ADMIN_E016",
        "ADMIN_E020",
        "ADMIN_E025",
        "ADMIN_E026",
        "ADMIN_E029"
    )
    @PostMapping("/change-requests/{id}/rollback")
    fun rollback(@PathVariable id: Long): ApiResponse<ChangeRequestActionResponse>
}