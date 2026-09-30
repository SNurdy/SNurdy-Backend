package com.snurdy.backend.domain.test.glossary.controller

import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCardsResponse
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitResponse
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

@Tag(name = "Glossary Test", description = "기본 단어장 테스트 API")
@RequestMapping("/v2/test/glossaries/{glossaryId}")
interface GlossaryTestApi {

    @Operation(summary = "기본 단어장 테스트 카드 조회")
    @ApiSuccessCode("TEST_CARD_S001")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E001")
    @PostMapping("/test-cards")
    fun listTestCards(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: GlossaryTestCardsRequest
    ): ApiResponse<GlossaryTestCardsResponse>

    @Operation(summary = "기본 단어장 테스트 결과 제출")
    @ApiSuccessCode("TEST_S002")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E002")
    @PostMapping("/test-submit")
    fun submit(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: GlossaryTestSubmitRequest
    ): ApiResponse<GlossaryTestSubmitResponse>
}