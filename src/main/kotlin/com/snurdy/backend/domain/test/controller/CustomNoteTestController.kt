package com.snurdy.backend.domain.test.controller

import com.snurdy.backend.domain.card.dto.CustomNoteCardsResponse
import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.card.service.CardService
import com.snurdy.backend.domain.test.dto.DistractorsRequest
import com.snurdy.backend.domain.test.dto.DistractorsResponse
import com.snurdy.backend.domain.test.dto.SubmitNoteTestResponse
import com.snurdy.backend.domain.test.dto.SubmitRequest
import com.snurdy.backend.domain.test.service.TestService
import com.snurdy.backend.global.constants.CardSuccess
import com.snurdy.backend.global.constants.TestSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CustomNoteTestController(
    private val cardService: CardService,
    private val testService: TestService
) : CustomNoteTestApi {

    override fun listCards(
        userId: UUID,
        customNoteId: Long,
        request: NoteCardsRequest
    ): ApiResponse<CustomNoteCardsResponse> =
        ApiResponse.success(
            CardSuccess.LIST_CARDS,
            cardService.listCustomNoteCards(userId, customNoteId, request)
        )

    override fun listDistractors(
        userId: UUID,
        customNoteId: Long,
        request: DistractorsRequest
    ): ApiResponse<DistractorsResponse> =
        ApiResponse.success(
            TestSuccess.LIST_DISTRACTORS,
            testService.listCustomNoteDistractors(userId, customNoteId, request)
        )

    override fun submit(
        userId: UUID,
        customNoteId: Long,
        request: SubmitRequest
    ): ApiResponse<SubmitNoteTestResponse> =
        ApiResponse.success(
            TestSuccess.SUBMIT_TEST,
            testService.submitCustomNoteTest(userId, customNoteId, request)
        )
}
