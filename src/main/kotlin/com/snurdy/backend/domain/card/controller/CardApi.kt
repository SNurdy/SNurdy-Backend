package com.snurdy.backend.domain.card.controller

import com.snurdy.backend.domain.card.dto.CardsRequest
import com.snurdy.backend.domain.card.dto.CardsResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(name = "Card", description = "v2 학습 카드 API")
@RequestMapping("/v2/cards")
interface CardApi {

    @Operation(summary = "학습 카드 조회")
    @ApiSuccessCode("CARD_S001")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E001")
    @PostMapping
    fun listCards(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: CardsRequest
    ): ApiResponse<CardsResponse>
}
