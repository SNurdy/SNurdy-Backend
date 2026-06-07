package com.snurdy.backend.domain.test.controller

import com.snurdy.backend.domain.card.dto.GlossaryCardsRequest
import com.snurdy.backend.domain.card.dto.GlossaryCardsResponse
import com.snurdy.backend.domain.card.service.CardService
import com.snurdy.backend.domain.test.dto.DistractorsRequest
import com.snurdy.backend.domain.test.dto.DistractorsResponse
import com.snurdy.backend.domain.test.dto.SubmitGlossaryTestResponse
import com.snurdy.backend.domain.test.dto.SubmitRequest
import com.snurdy.backend.domain.test.service.TestService
import com.snurdy.backend.global.constants.CardSuccess
import com.snurdy.backend.global.constants.TestSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class GlossaryTestController(
    private val cardService: CardService,
    private val testService: TestService
) : GlossaryTestApi {

    override fun listCards(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryCardsRequest
    ): ApiResponse<GlossaryCardsResponse> =
        ApiResponse.success(
            CardSuccess.LIST_CARDS,
            cardService.listGlossaryCards(userId, glossaryId, request)
        )

    override fun listDistractors(
        userId: UUID,
        glossaryId: Long,
        request: DistractorsRequest
    ): ApiResponse<DistractorsResponse> =
        ApiResponse.success(
            TestSuccess.LIST_DISTRACTORS,
            testService.listGlossaryDistractors(userId, glossaryId, request)
        )

    override fun submit(
        userId: UUID,
        glossaryId: Long,
        request: SubmitRequest
    ): ApiResponse<SubmitGlossaryTestResponse> =
        ApiResponse.success(
            TestSuccess.SUBMIT_TEST,
            testService.submitGlossaryTest(userId, glossaryId, request)
        )
}
