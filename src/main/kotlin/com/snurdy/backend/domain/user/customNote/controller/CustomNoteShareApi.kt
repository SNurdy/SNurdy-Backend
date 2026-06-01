package com.snurdy.backend.domain.user.customNote.controller

import com.snurdy.backend.domain.user.customNote.dto.CustomNoteResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteShareIssueResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@Tag(name = "Custom-Note-Share", description = "커스텀 단어장 공유 API")
@RequestMapping("/v2/custom-notes")
interface CustomNoteShareApi {

    @Operation(summary = "활성 공유 코드 조회. 본인 단어장만")
    @ApiSuccessCode("CUSTOM_NOTE_S015")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "CUSTOM_NOTE_E008")
    @GetMapping("/{noteId}/share")
    fun getShareCode(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long
    ): ApiResponse<CustomNoteShareIssueResponse>

    @Operation(summary = "공유 코드 발급. 이미 활성 코드 있으면 SHARE_CODE_ALREADY_EXISTS")
    @ApiSuccessCode("CUSTOM_NOTE_S011")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "CUSTOM_NOTE_E009", "CUSTOM_NOTE_E010")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{noteId}/share")
    fun issueShareCode(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long
    ): ApiResponse<CustomNoteShareIssueResponse>

    @Operation(summary = "공유 코드로 본인 단어장에 복제. 제목 충돌 시 (2) (3) suffix 자동")
    @ApiSuccessCode("CUSTOM_NOTE_S013")
    @ApiErrorCodes("CUSTOM_NOTE_E003", "CUSTOM_NOTE_E006", "CUSTOM_NOTE_E008")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/from-share/{code}")
    fun importByShareCode(
        @CurrentUserId userId: UUID,
        @PathVariable code: String
    ): ApiResponse<CustomNoteResponse>

    @Operation(summary = "공유 코드 비활성화. 코드 폐기 (감사 추적 없음)")
    @ApiSuccessCode("CUSTOM_NOTE_S014")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @DeleteMapping("/{noteId}/share")
    fun revokeShareCode(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long
    ): ApiResponse<Unit>
}
