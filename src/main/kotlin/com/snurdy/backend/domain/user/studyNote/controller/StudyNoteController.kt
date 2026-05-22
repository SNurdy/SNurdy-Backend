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
import com.snurdy.backend.domain.user.studyNote.service.StudyNoteService
import com.snurdy.backend.global.constants.StudyNoteSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StudyNoteController(
    private val studyNoteService: StudyNoteService
) : StudyNoteApi {

    override fun createNote(userId: UUID, request: CreateStudyNoteRequest): ApiResponse<StudyNoteResponse> =
        ApiResponse.success(StudyNoteSuccess.CREATE_NOTE, studyNoteService.create(userId, request))

    override fun listNotes(userId: UUID): ApiResponse<StudyNoteListResponse> =
        ApiResponse.success(StudyNoteSuccess.LIST_NOTES, studyNoteService.list(userId))

    override fun getNote(userId: UUID, noteId: Long): ApiResponse<StudyNoteResponse> =
        ApiResponse.success(StudyNoteSuccess.GET_NOTE, studyNoteService.get(userId, noteId))

    override fun updateNote(
        userId: UUID,
        noteId: Long,
        request: UpdateStudyNoteRequest
    ): ApiResponse<StudyNoteResponse> =
        ApiResponse.success(StudyNoteSuccess.UPDATE_NOTE, studyNoteService.update(userId, noteId, request))

    override fun deleteNote(userId: UUID, noteId: Long): ApiResponse<Unit> {
        studyNoteService.deleteNote(userId, noteId)
        return ApiResponse.success(StudyNoteSuccess.DELETE_NOTE)
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

    override fun deleteTerm(userId: UUID, noteId: Long, termId: Long): ApiResponse<Unit> {
        studyNoteService.deleteTerm(userId, noteId, termId)
        return ApiResponse.success(StudyNoteSuccess.DELETE_TERM)
    }

    override fun listTerms(
        userId: UUID,
        noteId: Long,
        page: Int,
        size: Int
    ): ApiResponse<StudyNoteTermsResponse> =
        ApiResponse.success(
            StudyNoteSuccess.LIST_TERMS,
            studyNoteService.listTerms(userId, noteId, page, size)
        )

    override fun deleteTerms(
        userId: UUID,
        noteId: Long,
        request: DeleteTermsFromStudyNoteRequest
    ): ApiResponse<Unit> {
        studyNoteService.deleteTerms(userId, noteId, request.termIds)
        return ApiResponse.success(StudyNoteSuccess.DELETE_TERMS)
    }

    override fun deleteNotes(
        userId: UUID,
        request: DeleteStudyNotesRequest
    ): ApiResponse<Unit> {
        studyNoteService.deleteNotes(userId, request.noteIds)
        return ApiResponse.success(StudyNoteSuccess.DELETE_NOTES)
    }
}
