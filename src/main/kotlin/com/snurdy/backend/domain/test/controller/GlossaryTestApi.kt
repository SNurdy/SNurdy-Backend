package com.snurdy.backend.domain.test.controller

import com.snurdy.backend.domain.card.dto.GlossaryCardsRequest
import com.snurdy.backend.domain.card.dto.GlossaryCardsResponse
import com.snurdy.backend.domain.test.dto.DistractorsRequest
import com.snurdy.backend.domain.test.dto.DistractorsResponse
import com.snurdy.backend.domain.test.dto.SubmitGlossaryTestResponse
import com.snurdy.backend.domain.test.dto.SubmitRequest
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

@Tag(name = "Glossary Test", description = "기본 단어장 학습/테스트 API")
@RequestMapping("/v2/test/glossaries/{glossaryId}")
interface GlossaryTestApi {

    @Operation(summary = "기본 단어장 학습 카드 조회")
    @ApiSuccessCode("CARD_S001")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E001")
    @PostMapping("/cards")
    fun listCards(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: GlossaryCardsRequest
    ): ApiResponse<GlossaryCardsResponse>

    @Operation(summary = "기본 단어장 다지선다 오답 보기 생성")
    @ApiSuccessCode("TEST_S001")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E001")
    @PostMapping("/distractors")
    fun listDistractors(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: DistractorsRequest
    ): ApiResponse<DistractorsResponse>

    @Operation(summary = "기본 단어장 테스트 결과 제출")
    @ApiSuccessCode("TEST_S002")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E002")
    @PostMapping("/submit")
    fun submit(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: SubmitRequest
    ): ApiResponse<SubmitGlossaryTestResponse>
}
