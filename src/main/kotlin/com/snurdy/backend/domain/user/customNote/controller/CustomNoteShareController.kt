package com.snurdy.backend.domain.user.customNote.controller

import com.snurdy.backend.domain.user.customNote.dto.CustomNoteResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteShareIssueResponse
import com.snurdy.backend.domain.user.customNote.service.CustomNoteImportService
import com.snurdy.backend.domain.user.customNote.service.CustomNoteShareService
import com.snurdy.backend.global.constants.CustomNoteSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CustomNoteShareController(
    private val customNoteShareService: CustomNoteShareService,
    private val customNoteImportService: CustomNoteImportService
) : CustomNoteShareApi {

    override fun issueShareCode(
        userId: UUID,
        noteId: Long
    ): ApiResponse<CustomNoteShareIssueResponse> =
        ApiResponse.success(
            CustomNoteSuccess.ISSUE_SHARE_CODE,
            customNoteShareService.issue(userId, noteId)
        )

    override fun importByShareCode(
        userId: UUID,
        code: String
    ): ApiResponse<CustomNoteResponse> =
        ApiResponse.success(
            CustomNoteSuccess.IMPORT_SHARED_NOTE,
            customNoteImportService.importByCode(userId, code)
        )

    override fun revokeShareCode(
        userId: UUID,
        noteId: Long
    ): ApiResponse<Unit> {
        customNoteShareService.revoke(userId, noteId)
        return ApiResponse.success(CustomNoteSuccess.REVOKE_SHARE_CODE)
    }
}
