package com.snurdy.backend.domain.test.glossary.controller

import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsResponse
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitResponse
import com.snurdy.backend.domain.test.glossary.service.GlossaryTestService
import com.snurdy.backend.global.constants.TestSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class GlossaryTestController(
    private val glossaryTestService: GlossaryTestService
) : GlossaryTestApi {

    override fun listTestCards(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryTestCardsRequest
    ): ApiResponse<GlossaryTestCardsResponse> =
        ApiResponse.success(
            TestSuccess.LIST_TEST_CARDS,
            glossaryTestService.listTestCards(userId, glossaryId, request)
        )

    override fun submit(
        userId: UUID,
        glossaryId: Long,
        idempotencyKey: UUID?,
        request: GlossaryTestSubmitRequest
    ): ApiResponse<GlossaryTestSubmitResponse> =
        ApiResponse.success(
            TestSuccess.SUBMIT_TEST,
            glossaryTestService.submitTest(userId, glossaryId, request, idempotencyKey)
        )
}