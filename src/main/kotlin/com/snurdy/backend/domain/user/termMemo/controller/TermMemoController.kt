package com.snurdy.backend.domain.user.termMemo.controller

import com.snurdy.backend.domain.user.termMemo.dto.TermMemoResponse
import com.snurdy.backend.domain.user.termMemo.dto.UpsertTermMemoRequest
import com.snurdy.backend.domain.user.termMemo.service.TermMemoService
import com.snurdy.backend.global.constants.TermMemoSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class TermMemoController(
    private val termMemoService: TermMemoService
) : TermMemoApi {

    override fun upsert(
        userId: UUID,
        termId: Long,
        request: UpsertTermMemoRequest
    ): ApiResponse<TermMemoResponse> =
        ApiResponse.success(
            TermMemoSuccess.UPSERT_MEMO,
            termMemoService.upsert(userId, termId, request.content)
        )

    override fun delete(userId: UUID, termId: Long): ApiResponse<Unit> {
        termMemoService.delete(userId, termId)
        return ApiResponse.success(TermMemoSuccess.DELETE_MEMO)
    }
}
