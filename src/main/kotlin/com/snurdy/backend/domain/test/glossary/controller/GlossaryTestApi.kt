package com.snurdy.backend.domain.test.glossary.controller

import com.snurdy.backend.domain.test.common.docs.TestSubmissionSwaggerDescription
import com.snurdy.backend.domain.test.glossary.docs.GlossaryTestSwaggerDescription
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsResponse
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(
    name = GlossaryTestSwaggerDescription.GLOSSARY_TEST_API_TAG_NAME,
    description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_API_TAG_DESCRIPTION
)
@RequestMapping("/v2/test/glossaries/{glossaryId}")
interface GlossaryTestApi {

    @Operation(summary = GlossaryTestSwaggerDescription.LIST_TEST_CARDS_SUMMARY)
    @ApiSuccessCode("TEST_S003")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E001")
    @PostMapping("/test-cards")
    fun listTestCards(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: GlossaryTestCardsRequest
    ): ApiResponse<GlossaryTestCardsResponse>

    @Operation(summary = GlossaryTestSwaggerDescription.SUBMIT_SUMMARY)
    @ApiSuccessCode("TEST_S002")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E002", "USER_E010")
    @PostMapping("/test-submit")
    fun submit(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @Parameter(description = TestSubmissionSwaggerDescription.IDEMPOTENCY_KEY)
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: UUID?,
        @RequestBody @Valid request: GlossaryTestSubmitRequest
    ): ApiResponse<GlossaryTestSubmitResponse>
}