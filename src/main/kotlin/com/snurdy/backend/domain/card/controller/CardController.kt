package com.snurdy.backend.domain.card.controller

import com.snurdy.backend.domain.card.dto.CardsRequest
import com.snurdy.backend.domain.card.dto.CardsResponse
import com.snurdy.backend.domain.card.service.CardService
import com.snurdy.backend.global.constants.CardSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CardController(
    private val cardService: CardService
) : CardApi {

    override fun listCards(
        userId: UUID,
        request: CardsRequest
    ): ApiResponse<CardsResponse> =
        ApiResponse.success(
            CardSuccess.LIST_CARDS,
            cardService.listCards(userId, request)
        )
}
