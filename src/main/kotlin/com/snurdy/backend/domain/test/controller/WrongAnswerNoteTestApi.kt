package com.snurdy.backend.domain.test.controller

import com.snurdy.backend.domain.card.dto.NoteCardsRequest
import com.snurdy.backend.domain.card.dto.WrongAnswerCardsResponse
import com.snurdy.backend.domain.test.dto.DistractorsRequest
import com.snurdy.backend.domain.test.dto.DistractorsResponse
import com.snurdy.backend.domain.test.dto.SubmitNoteTestResponse
import com.snurdy.backend.domain.test.dto.SubmitRequest
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriod
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

@Tag(name = "Wrong Answer Note Test", description = "오답노트 학습/테스트 API (by-period / by-glossary)")
@RequestMapping("/v2/test/wrong-answer-notes")
interface WrongAnswerNoteTestApi {

    @Operation(summary = "오답노트 by-period 학습 카드 조회")
    @ApiSuccessCode("CARD_S001")
    @ApiErrorCodes("WRONG_ANSWER_NOTE_E002", "TEST_E001")
    @PostMapping("/by-period/{period}/cards")
    fun listCardsByPeriod(
        @CurrentUserId userId: UUID,
        @PathVariable period: WrongAnswerPeriod,
        @RequestBody @Valid request: NoteCardsRequest
    ): ApiResponse<WrongAnswerCardsResponse>

    @Operation(summary = "오답노트 by-period 다지선다 오답 보기 생성")
    @ApiSuccessCode("TEST_S001")
    @ApiErrorCodes("WRONG_ANSWER_NOTE_E002", "TEST_E001", "TEST_E002")
    @PostMapping("/by-period/{period}/distractors")
    fun listDistractorsByPeriod(
        @CurrentUserId userId: UUID,
        @PathVariable period: WrongAnswerPeriod,
        @RequestBody @Valid request: DistractorsRequest
    ): ApiResponse<DistractorsResponse>

    @Operation(summary = "오답노트 by-period 테스트 결과 제출")
    @ApiSuccessCode("TEST_S002")
    @ApiErrorCodes("WRONG_ANSWER_NOTE_E002")
    @PostMapping("/by-period/{period}/submit")
    fun submitByPeriod(
        @CurrentUserId userId: UUID,
        @PathVariable period: WrongAnswerPeriod,
        @RequestBody @Valid request: SubmitRequest
    ): ApiResponse<SubmitNoteTestResponse>

    @Operation(summary = "오답노트 by-glossary 학습 카드 조회")
    @ApiSuccessCode("CARD_S001")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E001")
    @PostMapping("/by-glossary/{glossaryId}/cards")
    fun listCardsByGlossary(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: NoteCardsRequest
    ): ApiResponse<WrongAnswerCardsResponse>

    @Operation(summary = "오답노트 by-glossary 다지선다 오답 보기 생성")
    @ApiSuccessCode("TEST_S001")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E001", "TEST_E002")
    @PostMapping("/by-glossary/{glossaryId}/distractors")
    fun listDistractorsByGlossary(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: DistractorsRequest
    ): ApiResponse<DistractorsResponse>

    @Operation(summary = "오답노트 by-glossary 테스트 결과 제출")
    @ApiSuccessCode("TEST_S002")
    @ApiErrorCodes("DICTIONARY_E001")
    @PostMapping("/by-glossary/{glossaryId}/submit")
    fun submitByGlossary(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: SubmitRequest
    ): ApiResponse<SubmitNoteTestResponse>
}
