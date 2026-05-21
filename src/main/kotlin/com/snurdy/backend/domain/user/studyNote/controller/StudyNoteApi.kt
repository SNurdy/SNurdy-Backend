package com.snurdy.backend.domain.user.studyNote.controller

import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteListResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermsResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermDetailResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermSearchResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Validated
@Tag(name = "Study-Note", description = "사용자 학습 노트(단어장) API — 본인 진행도/북마크 컨텍스트 포함")
interface StudyNoteApi {

    @Operation(summary = "학습 노트 목록 조회")
    @ApiSuccessCode("STUDY_NOTE_S001")
    @GetMapping("/v1/study-notes")
    fun list(
        @CurrentUserId userId: UUID,
        @Parameter(description = "카테고리 필터")
        @RequestParam(required = false) category: String?
    ): ApiResponse<StudyNoteListResponse>

    @Operation(summary = "학습 노트 내 단어 목록 조회")
    @ApiSuccessCode("STUDY_NOTE_S002")
    @ApiErrorCodes("DICTIONARY_E001", "DICTIONARY_E005")
    @GetMapping("/v1/study-notes/{glossaryId}/terms")
    fun getTerms(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestParam(defaultValue = "all") filter: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<StudyNoteTermsResponse>

    @Operation(summary = "단어 검색 (북마크 컨텍스트 포함)")
    @ApiSuccessCode("STUDY_NOTE_S003")
    @GetMapping("/v1/study-notes/terms")
    fun searchTerms(
        @CurrentUserId userId: UUID,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<StudyNoteTermSearchResponse>

    @Operation(summary = "단어 상세 조회 (북마크 컨텍스트 포함)")
    @ApiSuccessCode("STUDY_NOTE_S004")
    @ApiErrorCodes("DICTIONARY_E002")
    @GetMapping("/v1/study-notes/terms/{termId}")
    fun getTermDetail(
        @CurrentUserId userId: UUID,
        @PathVariable termId: Long
    ): ApiResponse<StudyNoteTermDetailResponse>
}
