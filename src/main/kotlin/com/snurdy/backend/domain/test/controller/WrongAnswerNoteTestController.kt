package com.snurdy.backend.domain.test.controller

import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.card.dto.WrongAnswerCardsResponse
import com.snurdy.backend.domain.card.service.CardService
import com.snurdy.backend.domain.test.dto.SubmitNoteTestResponse
import com.snurdy.backend.domain.test.dto.SubmitRequest
import com.snurdy.backend.domain.test.service.TestService
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriod
import com.snurdy.backend.global.constants.CardSuccess
import com.snurdy.backend.global.constants.TestSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class WrongAnswerNoteTestController(
    private val cardService: CardService,
    private val testService: TestService
) : WrongAnswerNoteTestApi {

    override fun listCardsByPeriod(
        userId: UUID,
        period: WrongAnswerPeriod,
        request: NoteCardsRequest
    ): ApiResponse<WrongAnswerCardsResponse> =
        ApiResponse.success(
            CardSuccess.LIST_CARDS,
            cardService.listWrongAnswerByPeriodCards(userId, period, request)
        )

    override fun submitByPeriod(
        userId: UUID,
        period: WrongAnswerPeriod,
        request: SubmitRequest
    ): ApiResponse<SubmitNoteTestResponse> =
        ApiResponse.success(
            TestSuccess.SUBMIT_TEST,
            testService.submitWrongAnswerTest(userId, request)
        )

    override fun listCardsByGlossary(
        userId: UUID,
        glossaryId: Long,
        request: NoteCardsRequest
    ): ApiResponse<WrongAnswerCardsResponse> =
        ApiResponse.success(
            CardSuccess.LIST_CARDS,
            cardService.listWrongAnswerByGlossaryCards(userId, glossaryId, request)
        )

    override fun submitByGlossary(
        userId: UUID,
        glossaryId: Long,
        request: SubmitRequest
    ): ApiResponse<SubmitNoteTestResponse> =
        ApiResponse.success(
            TestSuccess.SUBMIT_TEST,
            testService.submitWrongAnswerTest(userId, request)
        )
}
