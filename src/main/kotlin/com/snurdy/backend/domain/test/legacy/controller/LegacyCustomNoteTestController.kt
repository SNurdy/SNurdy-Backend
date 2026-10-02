package com.snurdy.backend.domain.test.legacy.controller

import com.snurdy.backend.domain.card.dto.CustomNoteCardsResponse
import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.card.service.CardService
import com.snurdy.backend.domain.test.legacy.dto.SubmitNoteTestResponse
import com.snurdy.backend.domain.test.legacy.dto.SubmitRequest
import com.snurdy.backend.domain.test.legacy.service.LegacyTestService
import com.snurdy.backend.global.constants.CardSuccess
import com.snurdy.backend.global.constants.TestSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class LegacyCustomNoteTestController(
    private val cardService: CardService,
    private val legacyTestService: LegacyTestService
) : LegacyCustomNoteTestApi {

    override fun listCards(
        userId: UUID,
        customNoteId: Long,
        request: NoteCardsRequest
    ): ApiResponse<CustomNoteCardsResponse> =
        ApiResponse.success(
            CardSuccess.LIST_CARDS,
            cardService.listCustomNoteCards(userId, customNoteId, request)
        )

    override fun submit(
        userId: UUID,
        customNoteId: Long,
        request: SubmitRequest
    ): ApiResponse<SubmitNoteTestResponse> =
        ApiResponse.success(
            TestSuccess.SUBMIT_TEST,
            legacyTestService.submitCustomNoteTest(userId, customNoteId, request)
        )
}
