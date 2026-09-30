package com.snurdy.backend.domain.test.legacy.controller

import com.snurdy.backend.domain.card.dto.GlossaryCardsRequest
import com.snurdy.backend.domain.card.dto.GlossaryCardsResponse
import com.snurdy.backend.domain.card.service.CardService
import com.snurdy.backend.domain.test.legacy.dto.SubmitGlossaryTestResponse
import com.snurdy.backend.domain.test.legacy.dto.SubmitRequest
import com.snurdy.backend.domain.test.legacy.service.LegacyTestService
import com.snurdy.backend.global.constants.CardSuccess
import com.snurdy.backend.global.constants.TestSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class LegacyGlossaryTestController(
    private val cardService: CardService,
    private val legacyTestService: LegacyTestService
) : LegacyGlossaryTestApi {

    override fun listCards(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryCardsRequest
    ): ApiResponse<GlossaryCardsResponse> =
        ApiResponse.success(
            CardSuccess.LIST_CARDS,
            cardService.listGlossaryCards(userId, glossaryId, request)
        )

    override fun submit(
        userId: UUID,
        glossaryId: Long,
        request: SubmitRequest
    ): ApiResponse<SubmitGlossaryTestResponse> =
        ApiResponse.success(
            TestSuccess.SUBMIT_TEST,
            legacyTestService.submitGlossaryTest(userId, glossaryId, request)
        )
}