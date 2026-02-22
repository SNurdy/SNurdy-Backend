package com.snurdy.backend.domain.admin.changeset.controller

import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetDetailResponse
import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetListResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Admin-ChangeSet-Query", description = "관리자 변경 요청 조회 API")
@RequestMapping("/v1/admin/change-sets")
interface AdminChangeSetQueryApi {

    @Operation(summary = "변경 요청 목록 조회")
    @GetMapping
    fun getChangeSets(): ApiResponse<ChangeSetListResponse>

    @Operation(summary = "변경 요청 상세 조회")
    @GetMapping("/{changeSetId}")
    fun getChangeSetDetail(
        @PathVariable changeSetId: Long
    ): ApiResponse<ChangeSetDetailResponse>
}
