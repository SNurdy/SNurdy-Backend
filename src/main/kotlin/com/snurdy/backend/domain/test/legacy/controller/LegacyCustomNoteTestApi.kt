package com.snurdy.backend.domain.test.legacy.controller

import com.snurdy.backend.domain.card.dto.CustomNoteCardsResponse
import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.test.legacy.dto.SubmitNoteTestResponse
import com.snurdy.backend.domain.test.legacy.dto.SubmitRequest
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(name = "Custom Note Test", description = "커스텀노트 학습/테스트 API")
@RequestMapping("/v2/test/custom-notes/{customNoteId}")
interface LegacyCustomNoteTestApi {

    @Operation(summary = "커스텀노트 학습 카드 조회")
    @ApiSuccessCode("CARD_S001")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "TEST_E001")
    @PostMapping("/cards")
    fun listCards(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long,
        @RequestBody @Valid request: NoteCardsRequest
    ): ApiResponse<CustomNoteCardsResponse>

    @Operation(summary = "커스텀노트 테스트 결과 제출")
    @ApiSuccessCode("TEST_S002")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @PostMapping("/submit")
    fun submit(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long,
        @RequestBody @Valid request: SubmitRequest
    ): ApiResponse<SubmitNoteTestResponse>
}
