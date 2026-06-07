package com.snurdy.backend.domain.user.wrongAnswerNote.controller

import com.snurdy.backend.domain.user.wrongAnswerNote.dto.DeleteTermsFromWrongAnswerNoteRequest
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerGlossaryCardListResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriod
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriodCardListResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerTermsResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Validated
@Tag(name = "Wrong Answer Note", description = "오답노트 API")
@RequestMapping("/v2/wrong-answer-note")
interface WrongAnswerNoteApi {

    @Operation(summary = "기간별 오답노트 카드 목록 (오늘/이번 주/전체)")
    @ApiSuccessCode("WRONG_ANSWER_NOTE_S001")
    @GetMapping("/by-period")
    fun listByPeriod(
        @CurrentUserId userId: UUID
    ): ApiResponse<WrongAnswerPeriodCardListResponse>

    @Operation(summary = "과별 오답노트 카드 목록")
    @ApiSuccessCode("WRONG_ANSWER_NOTE_S002")
    @GetMapping("/by-glossary")
    fun listByGlossary(
        @CurrentUserId userId: UUID
    ): ApiResponse<WrongAnswerGlossaryCardListResponse>

    @Operation(summary = "오늘 틀린 단어 목록 (검색 + 페이징)")
    @ApiSuccessCode("WRONG_ANSWER_NOTE_S003")
    @GetMapping("/today/terms")
    fun listTodayTerms(
        @CurrentUserId userId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) query: String?
    ): ApiResponse<WrongAnswerTermsResponse>

    @Operation(summary = "이번 주 틀린 단어 목록 (자연주 - 월요일 기준, 검색 + 페이징)")
    @ApiSuccessCode("WRONG_ANSWER_NOTE_S003")
    @GetMapping("/week/terms")
    fun listWeekTerms(
        @CurrentUserId userId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) query: String?
    ): ApiResponse<WrongAnswerTermsResponse>

    @Operation(summary = "이번 달 틀린 단어 목록 (자연월 - 1일 기준, 검색 + 페이징)")
    @ApiSuccessCode("WRONG_ANSWER_NOTE_S003")
    @GetMapping("/month/terms")
    fun listMonthTerms(
        @CurrentUserId userId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) query: String?
    ): ApiResponse<WrongAnswerTermsResponse>

    @Operation(summary = "전체 틀린 단어 목록 (검색 + 페이징)")
    @ApiSuccessCode("WRONG_ANSWER_NOTE_S003")
    @GetMapping("/all/terms")
    fun listAllTerms(
        @CurrentUserId userId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) query: String?
    ): ApiResponse<WrongAnswerTermsResponse>

    @Operation(summary = "과별 오답 단어 목록 (검색 + 페이징)")
    @ApiSuccessCode("WRONG_ANSWER_NOTE_S004")
    @ApiErrorCodes("DICTIONARY_E001")
    @GetMapping("/by-glossary/{glossaryId}/terms")
    fun listGlossaryTerms(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(required = false) query: String?
    ): ApiResponse<WrongAnswerTermsResponse>

    @Operation(summary = "오답 단어 다중 삭제 (soft delete). 입력 단어가 모두 노트에 활성 상태로 있어야 처리")
    @ApiSuccessCode("WRONG_ANSWER_NOTE_S005")
    @ApiErrorCodes("WRONG_ANSWER_NOTE_E001")
    @DeleteMapping("/terms")
    fun deleteTerms(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: DeleteTermsFromWrongAnswerNoteRequest
    ): ApiResponse<Unit>
}
