package com.snurdy.backend.domain.user.studyNote.controller

import com.snurdy.backend.domain.user.studyNote.dto.AddTermsResult
import com.snurdy.backend.domain.user.studyNote.dto.AddTermsToStudyNoteRequest
import com.snurdy.backend.domain.user.studyNote.dto.CreateStudyNoteRequest
import com.snurdy.backend.domain.user.studyNote.dto.DeleteStudyNotesRequest
import com.snurdy.backend.domain.user.studyNote.dto.DeleteTermsFromStudyNoteRequest
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteListResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermsResponse
import com.snurdy.backend.domain.user.studyNote.dto.UpdateStudyNoteRequest
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
import java.util.UUID

@Validated
@Tag(name = "Study-Note", description = "사용자 커스텀 단어장 API")
@RequestMapping("/v2/study-notes")
interface StudyNoteApi {

    @Operation(summary = "커스텀 단어장 생성")
    @ApiSuccessCode("STUDY_NOTE_S001")
    @ApiErrorCodes("STUDY_NOTE_E003", "STUDY_NOTE_E006")
    @PostMapping
    fun createNote(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: CreateStudyNoteRequest
    ): ApiResponse<StudyNoteResponse>

    @Operation(summary = "내 커스텀 단어장 목록 조회 (최근 생성 순)")
    @ApiSuccessCode("STUDY_NOTE_S002")
    @GetMapping
    fun listNotes(@CurrentUserId userId: UUID): ApiResponse<StudyNoteListResponse>

    @Operation(summary = "커스텀 단어장 상세 조회")
    @ApiSuccessCode("STUDY_NOTE_S003")
    @ApiErrorCodes("STUDY_NOTE_E001", "STUDY_NOTE_E002", "STUDY_NOTE_E005")
    @GetMapping("/{noteId}")
    fun getNote(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long
    ): ApiResponse<StudyNoteResponse>

    @Operation(summary = "커스텀 단어장 제목 수정")
    @ApiSuccessCode("STUDY_NOTE_S004")
    @ApiErrorCodes("STUDY_NOTE_E001", "STUDY_NOTE_E002", "STUDY_NOTE_E005", "STUDY_NOTE_E006")
    @PatchMapping("/{noteId}")
    fun updateNote(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @RequestBody @Valid request: UpdateStudyNoteRequest
    ): ApiResponse<StudyNoteResponse>

    @Operation(summary = "커스텀 단어장 단일 삭제")
    @ApiSuccessCode("STUDY_NOTE_S005")
    @ApiErrorCodes("STUDY_NOTE_E001", "STUDY_NOTE_E002", "STUDY_NOTE_E005")
    @DeleteMapping("/{noteId}")
    fun deleteNote(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long
    ): ApiResponse<Unit>

    @Operation(summary = "커스텀 단어장에 사전 단어 추가 (배치)")
    @ApiSuccessCode("STUDY_NOTE_S006")
    @ApiErrorCodes(
        "STUDY_NOTE_E001",
        "STUDY_NOTE_E002",
        "STUDY_NOTE_E004",
        "STUDY_NOTE_E005",
        "DICTIONARY_E002"
    )
    @PostMapping("/{noteId}/terms")
    fun addTerms(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @RequestBody @Valid request: AddTermsToStudyNoteRequest
    ): ApiResponse<AddTermsResult>

    @Operation(summary = "커스텀 단어장에서 단어 단일 삭제")
    @ApiSuccessCode("STUDY_NOTE_S007")
    @ApiErrorCodes(
        "STUDY_NOTE_E001",
        "STUDY_NOTE_E002",
        "STUDY_NOTE_E005",
        "DICTIONARY_E004"
    )
    @DeleteMapping("/{noteId}/terms/{termId}")
    fun deleteTerm(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @PathVariable termId: Long
    ): ApiResponse<Unit>

    @Operation(summary = "커스텀 단어장 내 단어 목록 조회 (페이징)")
    @ApiSuccessCode("STUDY_NOTE_S008")
    @ApiErrorCodes("STUDY_NOTE_E001", "STUDY_NOTE_E002", "STUDY_NOTE_E005")
    @GetMapping("/{noteId}/terms")
    fun listTerms(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<StudyNoteTermsResponse>

    @Operation(summary = "커스텀 단어장에서 단어 다중 삭제. 입력한 단어가 단어장에 모두 있어야 처리")
    @ApiSuccessCode("STUDY_NOTE_S009")
    @ApiErrorCodes("STUDY_NOTE_E001", "STUDY_NOTE_E002", "STUDY_NOTE_E005", "DICTIONARY_E004")
    @DeleteMapping("/{noteId}/terms")
    fun deleteTerms(
        @CurrentUserId userId: UUID,
        @PathVariable noteId: Long,
        @RequestBody @Valid request: DeleteTermsFromStudyNoteRequest
    ): ApiResponse<Unit>

    @Operation(summary = "커스텀 단어장 다중 삭제. 모두 본인 소유 CUSTOM 이어야 처리")
    @ApiSuccessCode("STUDY_NOTE_S010")
    @ApiErrorCodes("STUDY_NOTE_E001", "STUDY_NOTE_E002", "STUDY_NOTE_E005")
    @DeleteMapping
    fun deleteNotes(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: DeleteStudyNotesRequest
    ): ApiResponse<Unit>
}
