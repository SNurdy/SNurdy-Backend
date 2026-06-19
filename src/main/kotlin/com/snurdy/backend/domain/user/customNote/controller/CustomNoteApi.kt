package com.snurdy.backend.domain.user.customNote.controller

import com.snurdy.backend.domain.user.customNote.dto.AddTermToCustomNotesRequest
import com.snurdy.backend.domain.user.customNote.dto.AddTermToCustomNotesResponse
import com.snurdy.backend.domain.user.customNote.dto.AddTermsToCustomNoteResponse
import com.snurdy.backend.domain.user.customNote.dto.AddTermsToCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.CreateCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.DeleteCustomNotesRequest
import com.snurdy.backend.domain.user.customNote.dto.DeleteTermsFromCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteListResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteTermsResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNotesForTermResponse
import com.snurdy.backend.domain.user.customNote.dto.UpdateCustomNoteRequest
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.http.HttpStatus
import java.util.UUID

@Validated
@Tag(name = "Custom Note", description = "사용자 커스텀 단어장 API")
@RequestMapping("/v2/custom-notes")
interface CustomNoteApi {

    @Operation(summary = "커스텀 단어장 생성")
    @ApiSuccessCode("CUSTOM_NOTE_S001")
    @ApiErrorCodes("CUSTOM_NOTE_E003", "CUSTOM_NOTE_E006")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun createNote(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: CreateCustomNoteRequest
    ): ApiResponse<CustomNoteResponse>

    @Operation(summary = "내 커스텀 단어장 목록 조회")
    @ApiSuccessCode("CUSTOM_NOTE_S002")
    @GetMapping
    fun listNotes(@CurrentUserId userId: UUID): ApiResponse<CustomNoteListResponse>

    @Operation(summary = "커스텀 단어장 상세 조회")
    @ApiSuccessCode("CUSTOM_NOTE_S003")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @GetMapping("/{customNoteId}")
    fun getNote(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long
    ): ApiResponse<CustomNoteResponse>

    @Operation(summary = "커스텀 단어장 제목 수정")
    @ApiSuccessCode("CUSTOM_NOTE_S004")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "CUSTOM_NOTE_E006")
    @PatchMapping("/{customNoteId}")
    fun updateNote(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long,
        @RequestBody @Valid request: UpdateCustomNoteRequest
    ): ApiResponse<CustomNoteResponse>

    @Operation(summary = "커스텀 단어장 단일 삭제")
    @ApiSuccessCode("CUSTOM_NOTE_S005")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @DeleteMapping("/{customNoteId}")
    fun deleteNote(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long
    ): ApiResponse<Unit>

    @Operation(summary = "단어 기준 내 커스텀 단어장 보유 현황 조회")
    @ApiSuccessCode("CUSTOM_NOTE_S017")
    @GetMapping("/terms/{glossaryId}/{termId}")
    fun listNotesForTerm(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @PathVariable termId: Long
    ): ApiResponse<CustomNotesForTermResponse>

    @Operation(summary = "사전 단어 1개를 N개 커스텀 단어장에 동시 추가")
    @ApiSuccessCode("CUSTOM_NOTE_S016")
    @ApiErrorCodes(
        "CUSTOM_NOTE_E001",
        "CUSTOM_NOTE_E002",
        "CUSTOM_NOTE_E004",
        "DICTIONARY_E001",
        "DICTIONARY_E002"
    )
    @PostMapping("/terms")
    fun addTermToNotes(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: AddTermToCustomNotesRequest
    ): ApiResponse<AddTermToCustomNotesResponse>

    @Operation(summary = "커스텀 단어장에 사전 단어 추가")
    @ApiSuccessCode("CUSTOM_NOTE_S006")
    @ApiErrorCodes(
        "CUSTOM_NOTE_E001",
        "CUSTOM_NOTE_E002",
        "CUSTOM_NOTE_E004",
        "DICTIONARY_E002"
    )
    @PostMapping("/{customNoteId}/terms")
    fun addTerms(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long,
        @RequestBody @Valid request: AddTermsToCustomNoteRequest
    ): ApiResponse<AddTermsToCustomNoteResponse>

    @Operation(summary = "커스텀 단어장에서 단어 단일 삭제")
    @ApiSuccessCode("CUSTOM_NOTE_S007")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "CUSTOM_NOTE_E007")
    @DeleteMapping("/{customNoteId}/terms/{glossaryId}/{termId}")
    fun deleteTerm(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long,
        @PathVariable glossaryId: Long,
        @PathVariable termId: Long
    ): ApiResponse<Unit>

    @Operation(summary = "커스텀 단어장 내 단어 목록 조회")
    @ApiSuccessCode("CUSTOM_NOTE_S008")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @GetMapping("/{customNoteId}/terms")
    fun listTerms(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<CustomNoteTermsResponse>

    @Operation(summary = "커스텀 단어장에서 단어 다중 삭제")
    @ApiSuccessCode("CUSTOM_NOTE_S009")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "CUSTOM_NOTE_E007")
    @DeleteMapping("/{customNoteId}/terms")
    fun deleteTerms(
        @CurrentUserId userId: UUID,
        @PathVariable customNoteId: Long,
        @RequestBody @Valid request: DeleteTermsFromCustomNoteRequest
    ): ApiResponse<Unit>

    @Operation(summary = "커스텀 단어장 다중 삭제")
    @ApiSuccessCode("CUSTOM_NOTE_S010")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @DeleteMapping
    fun deleteNotes(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: DeleteCustomNotesRequest
    ): ApiResponse<Unit>
}
