package com.snurdy.backend.domain.admin.changeset.controller

import com.snurdy.backend.domain.admin.changeset.dto.ChangeSetItemCommandResponse
import com.snurdy.backend.domain.admin.changeset.dto.CreateChangeSetRequest
import com.snurdy.backend.domain.admin.changeset.dto.CreateChangeSetResponse
import com.snurdy.backend.domain.admin.changeset.dto.UpsertTermChangeSetItemRequest
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin-ChangeSet-Command", description = "관리자 변경 요청 커맨드 API")
@RequestMapping("/v1/admin/change-sets")
interface AdminChangeSetCommandApi {

    @Operation(summary = "변경 요청 생성")
    @PostMapping
    fun createChangeSet(
        @Valid @RequestBody request: CreateChangeSetRequest
    ): ApiResponse<CreateChangeSetResponse>

    @Operation(summary = "단어 변경 항목 추가")
    @PostMapping("/{changeSetId}/items/term")
    fun addTermItem(
        @PathVariable changeSetId: Long,
        @Valid @RequestBody request: UpsertTermChangeSetItemRequest
    ): ApiResponse<ChangeSetItemCommandResponse>

    @Operation(summary = "단어 변경 항목 수정")
    @PutMapping("/{changeSetId}/items/{itemId}/term")
    fun updateTermItem(
        @PathVariable changeSetId: Long,
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpsertTermChangeSetItemRequest
    ): ApiResponse<ChangeSetItemCommandResponse>

    @Operation(summary = "변경 항목 삭제")
    @DeleteMapping("/{changeSetId}/items/{itemId}")
    fun deleteItem(
        @PathVariable changeSetId: Long,
        @PathVariable itemId: Long
    ): ApiResponse<Unit>
}
