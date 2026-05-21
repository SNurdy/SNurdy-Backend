package com.snurdy.backend.domain.user.studyNote.controller

import com.snurdy.backend.domain.user.studyNote.dto.AddTermsResult
import com.snurdy.backend.domain.user.studyNote.dto.AddTermsToStudyNoteRequest
import com.snurdy.backend.domain.user.studyNote.dto.CreateStudyNoteRequest
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteListResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteResponse
import com.snurdy.backend.domain.user.studyNote.dto.UpdateStudyNoteRequest
import com.snurdy.backend.domain.user.studyNote.service.StudyNoteService
import com.snurdy.backend.global.constants.StudyNoteSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StudyNoteController(
    private val studyNoteService: StudyNoteService
) : StudyNoteApi {

    override fun create(userId: UUID, request: CreateStudyNoteRequest): ApiResponse<StudyNoteResponse> =
        ApiResponse.success(StudyNoteSuccess.CREATE, studyNoteService.create(userId, request))

    override fun list(userId: UUID): ApiResponse<StudyNoteListResponse> =
        ApiResponse.success(StudyNoteSuccess.LIST, studyNoteService.list(userId))

    override fun get(userId: UUID, noteId: Long): ApiResponse<StudyNoteResponse> =
        ApiResponse.success(StudyNoteSuccess.GET, studyNoteService.get(userId, noteId))

    override fun update(
        userId: UUID,
        noteId: Long,
        request: UpdateStudyNoteRequest
    ): ApiResponse<StudyNoteResponse> =
        ApiResponse.success(StudyNoteSuccess.UPDATE, studyNoteService.update(userId, noteId, request))

    override fun delete(userId: UUID, noteId: Long): ApiResponse<Unit> {
        studyNoteService.delete(userId, noteId)
        return ApiResponse.success(StudyNoteSuccess.DELETE)
    }

    override fun addTerms(
        userId: UUID,
        noteId: Long,
        request: AddTermsToStudyNoteRequest
    ): ApiResponse<AddTermsResult> =
        ApiResponse.success(
            StudyNoteSuccess.ADD_TERMS,
            studyNoteService.addTerms(userId, noteId, request.termIds)
        )

    override fun removeTerm(userId: UUID, noteId: Long, termId: Long): ApiResponse<Unit> {
        studyNoteService.removeTerm(userId, noteId, termId)
        return ApiResponse.success(StudyNoteSuccess.REMOVE_TERM)
    }
}
