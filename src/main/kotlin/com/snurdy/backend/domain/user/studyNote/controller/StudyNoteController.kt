package com.snurdy.backend.domain.user.studyNote.controller

import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteListResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermDetailResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermSearchResponse
import com.snurdy.backend.domain.user.studyNote.dto.StudyNoteTermsResponse
import com.snurdy.backend.domain.user.studyNote.service.StudyNoteService
import com.snurdy.backend.global.constants.StudyNoteSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StudyNoteController(
    private val studyNoteService: StudyNoteService
) : StudyNoteApi {
    override fun list(userId: UUID, category: String?): ApiResponse<StudyNoteListResponse> =
        ApiResponse.success(
            StudyNoteSuccess.LIST_NOTES,
            studyNoteService.list(userId, category)
        )

    override fun getTerms(
        userId: UUID,
        glossaryId: Long,
        filter: String,
        page: Int,
        size: Int
    ): ApiResponse<StudyNoteTermsResponse> =
        ApiResponse.success(
            StudyNoteSuccess.GET_NOTE_TERMS,
            studyNoteService.getTerms(
                userId = userId,
                glossaryId = glossaryId,
                filter = filter,
                page = page,
                size = size
            )
        )

    override fun searchTerms(userId: UUID, query: String?, page: Int, size: Int): ApiResponse<StudyNoteTermSearchResponse> =
        ApiResponse.success(
            StudyNoteSuccess.SEARCH_TERMS,
            studyNoteService.searchTerms(userId, query, page, size)
        )

    override fun getTermDetail(userId: UUID, termId: Long): ApiResponse<StudyNoteTermDetailResponse> =
        ApiResponse.success(
            StudyNoteSuccess.GET_TERM_DETAIL,
            studyNoteService.getTermDetail(userId, termId)
        )
}
