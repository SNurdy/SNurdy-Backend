package com.snurdy.backend.domain.user.termMemo.controller

import com.snurdy.backend.domain.user.termMemo.dto.TermMemoResponse
import com.snurdy.backend.domain.user.termMemo.dto.UpsertTermMemoRequest
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(name = "Term-Memo", description = "단어 메모 API (사용자 본인 메모 작성/수정/삭제)")
@RequestMapping("/v2/terms/{termId}/memo")
interface TermMemoApi {

    @Operation(summary = "단어 메모 작성 또는 수정 (upsert)")
    @ApiSuccessCode("TERM_MEMO_S001")
    @ApiErrorCodes("USER_E010", "DICTIONARY_E002")
    @PutMapping
    fun upsert(
        @CurrentUserId userId: UUID,
        @PathVariable termId: Long,
        @RequestBody @Valid request: UpsertTermMemoRequest
    ): ApiResponse<TermMemoResponse>

    @Operation(summary = "단어 메모 삭제")
    @ApiSuccessCode("TERM_MEMO_S002")
    @ApiErrorCodes("TERM_MEMO_E001")
    @DeleteMapping
    fun delete(
        @CurrentUserId userId: UUID,
        @PathVariable termId: Long
    ): ApiResponse<Unit>
}
