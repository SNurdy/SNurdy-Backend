package com.snurdy.backend.domain.user.customNote.controller

import com.snurdy.backend.domain.user.customNote.dto.AddTermsResponse
import com.snurdy.backend.domain.user.customNote.dto.AddTermsToCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.CreateCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.DeleteCustomNotesRequest
import com.snurdy.backend.domain.user.customNote.dto.DeleteTermsFromCustomNoteRequest
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteListResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteResponse
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteTermsResponse
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

    @Operation(summary = "내 커스텀 단어장 목록 조회 (최근 생성 순)")
    @ApiSuccessCode("CUSTOM_NOTE_S002")
    @GetMapping
    fun listNotes(@CurrentUserId userId: UUID): ApiResponse<CustomNoteListResponse>

    @Operation(summary = "커스텀 단어장 상세 조회")
    @ApiSuccessCode("CUSTOM_NOTE_S003")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @GetMapping("/{noteId}")
    fun getNote(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long
    ): ApiResponse<CustomNoteResponse>

    @Operation(summary = "커스텀 단어장 제목 수정")
    @ApiSuccessCode("CUSTOM_NOTE_S004")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "CUSTOM_NOTE_E006")
    @PatchMapping("/{noteId}")
    fun updateNote(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @RequestBody @Valid request: UpdateCustomNoteRequest
    ): ApiResponse<CustomNoteResponse>

    @Operation(summary = "커스텀 단어장 단일 삭제")
    @ApiSuccessCode("CUSTOM_NOTE_S005")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @DeleteMapping("/{noteId}")
    fun deleteNote(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long
    ): ApiResponse<Unit>

    @Operation(summary = "커스텀 단어장에 사전 단어 추가 (배치)")
    @ApiSuccessCode("CUSTOM_NOTE_S006")
    @ApiErrorCodes(
        "CUSTOM_NOTE_E001",
        "CUSTOM_NOTE_E002",
        "CUSTOM_NOTE_E004",
        "DICTIONARY_E002"
    )
    @PostMapping("/{noteId}/terms")
    fun addTerms(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @RequestBody @Valid request: AddTermsToCustomNoteRequest
    ): ApiResponse<AddTermsResponse>

    @Operation(summary = "커스텀 단어장에서 단어 단일 삭제. path 의 (glossaryId, termId) 쌍으로 식별")
    @ApiSuccessCode("CUSTOM_NOTE_S007")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "CUSTOM_NOTE_E007")
    @DeleteMapping("/{noteId}/terms/{glossaryId}/{termId}")
    fun deleteTerm(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @PathVariable glossaryId: Long,
        @PathVariable termId: Long
    ): ApiResponse<Unit>

    @Operation(summary = "커스텀 단어장 내 단어 목록 조회 (페이징)")
    @ApiSuccessCode("CUSTOM_NOTE_S008")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @GetMapping("/{noteId}/terms")
    fun listTerms(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<CustomNoteTermsResponse>

    @Operation(summary = "커스텀 단어장에서 단어 다중 삭제. 입력한 단어가 단어장에 모두 있어야 처리")
    @ApiSuccessCode("CUSTOM_NOTE_S009")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002", "CUSTOM_NOTE_E007")
    @DeleteMapping("/{noteId}/terms")
    fun deleteTerms(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @RequestBody @Valid request: DeleteTermsFromCustomNoteRequest
    ): ApiResponse<Unit>

    @Operation(summary = "커스텀 단어장 다중 삭제. 모두 본인 소유여야 처리")
    @ApiSuccessCode("CUSTOM_NOTE_S010")
    @ApiErrorCodes("CUSTOM_NOTE_E001", "CUSTOM_NOTE_E002")
    @DeleteMapping
    fun deleteNotes(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: DeleteCustomNotesRequest
    ): ApiResponse<Unit>
}
