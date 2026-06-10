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
import com.snurdy.backend.domain.user.customNote.service.CustomNoteService
import com.snurdy.backend.global.constants.CustomNoteSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CustomNoteController(
    private val customNoteService: CustomNoteService
) : CustomNoteApi {

    override fun createNote(userId: UUID, request: CreateCustomNoteRequest): ApiResponse<CustomNoteResponse> =
        ApiResponse.success(CustomNoteSuccess.CREATE_NOTE, customNoteService.create(userId, request))

    override fun listNotes(userId: UUID): ApiResponse<CustomNoteListResponse> =
        ApiResponse.success(CustomNoteSuccess.LIST_NOTES, customNoteService.list(userId))

    override fun getNote(userId: UUID, noteId: Long): ApiResponse<CustomNoteResponse> =
        ApiResponse.success(CustomNoteSuccess.GET_NOTE, customNoteService.get(userId, noteId))

    override fun updateNote(
        userId: UUID,
        noteId: Long,
        request: UpdateCustomNoteRequest
    ): ApiResponse<CustomNoteResponse> =
        ApiResponse.success(CustomNoteSuccess.UPDATE_NOTE, customNoteService.update(userId, noteId, request))

    override fun deleteNote(userId: UUID, noteId: Long): ApiResponse<Unit> {
        customNoteService.deleteNote(userId, noteId)
        return ApiResponse.success(CustomNoteSuccess.DELETE_NOTE)
    }

    override fun listNotesForTerm(
        userId: UUID,
        glossaryId: Long,
        termId: Long
    ): ApiResponse<CustomNotesForTermResponse> =
        ApiResponse.success(
            CustomNoteSuccess.LIST_NOTES_FOR_TERM,
            customNoteService.listNotesForTerm(userId, glossaryId, termId)
        )

    override fun addTermToNotes(
        userId: UUID,
        request: AddTermToCustomNotesRequest
    ): ApiResponse<AddTermToCustomNotesResponse> =
        ApiResponse.success(
            CustomNoteSuccess.ADD_TERM_TO_NOTES,
            customNoteService.addTermToNotes(userId, request.glossaryId, request.termId, request.customNoteIds)
        )

    override fun addTerms(
        userId: UUID,
        noteId: Long,
        request: AddTermsToCustomNoteRequest
    ): ApiResponse<AddTermsToCustomNoteResponse> =
        ApiResponse.success(
            CustomNoteSuccess.ADD_TERMS,
            customNoteService.addTerms(userId, noteId, request.items)
        )

    override fun deleteTerm(userId: UUID, noteId: Long, glossaryId: Long, termId: Long): ApiResponse<Unit> {
        customNoteService.deleteTerm(userId, noteId, glossaryId, termId)
        return ApiResponse.success(CustomNoteSuccess.DELETE_TERM)
    }

    override fun listTerms(
        userId: UUID,
        noteId: Long,
        page: Int,
        size: Int
    ): ApiResponse<CustomNoteTermsResponse> =
        ApiResponse.success(
            CustomNoteSuccess.LIST_TERMS,
            customNoteService.listTerms(userId, noteId, page, size)
        )

    override fun deleteTerms(
        userId: UUID,
        noteId: Long,
        request: DeleteTermsFromCustomNoteRequest
    ): ApiResponse<Unit> {
        customNoteService.deleteTerms(userId, noteId, request.items)
        return ApiResponse.success(CustomNoteSuccess.DELETE_TERMS)
    }

    override fun deleteNotes(
        userId: UUID,
        request: DeleteCustomNotesRequest
    ): ApiResponse<Unit> {
        customNoteService.deleteNotes(userId, request.customNoteIds)
        return ApiResponse.success(CustomNoteSuccess.DELETE_NOTES)
    }
}
