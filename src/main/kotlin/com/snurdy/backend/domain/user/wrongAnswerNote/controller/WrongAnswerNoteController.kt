package com.snurdy.backend.domain.user.wrongAnswerNote.controller

import com.snurdy.backend.domain.user.wrongAnswerNote.dto.DeleteTermsFromWrongAnswerNoteRequest
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerGlossaryCardListResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriod
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriodCardListResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerTermsResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.service.WrongAnswerNoteService
import com.snurdy.backend.global.constants.WrongAnswerNoteSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class WrongAnswerNoteController(
    private val wrongAnswerNoteService: WrongAnswerNoteService
) : WrongAnswerNoteApi {

    override fun listByPeriod(userId: UUID): ApiResponse<WrongAnswerPeriodCardListResponse> =
        ApiResponse.success(WrongAnswerNoteSuccess.LIST_BY_PERIOD, wrongAnswerNoteService.listByPeriod(userId))

    override fun listByGlossary(userId: UUID): ApiResponse<WrongAnswerGlossaryCardListResponse> =
        ApiResponse.success(WrongAnswerNoteSuccess.LIST_BY_GLOSSARY, wrongAnswerNoteService.listByGlossary(userId))

    override fun listTodayTerms(
        userId: UUID,
        page: Int,
        size: Int,
        query: String?
    ): ApiResponse<WrongAnswerTermsResponse> =
        ApiResponse.success(
            WrongAnswerNoteSuccess.LIST_PERIOD_TERMS,
            wrongAnswerNoteService.listPeriodTerms(userId, WrongAnswerPeriod.TODAY, page, size, query)
        )

    override fun listWeekTerms(
        userId: UUID,
        page: Int,
        size: Int,
        query: String?
    ): ApiResponse<WrongAnswerTermsResponse> =
        ApiResponse.success(
            WrongAnswerNoteSuccess.LIST_PERIOD_TERMS,
            wrongAnswerNoteService.listPeriodTerms(userId, WrongAnswerPeriod.WEEK, page, size, query)
        )

    override fun listMonthTerms(
        userId: UUID,
        page: Int,
        size: Int,
        query: String?
    ): ApiResponse<WrongAnswerTermsResponse> =
        ApiResponse.success(
            WrongAnswerNoteSuccess.LIST_PERIOD_TERMS,
            wrongAnswerNoteService.listPeriodTerms(userId, WrongAnswerPeriod.MONTH, page, size, query)
        )

    override fun listAllTerms(
        userId: UUID,
        page: Int,
        size: Int,
        query: String?
    ): ApiResponse<WrongAnswerTermsResponse> =
        ApiResponse.success(
            WrongAnswerNoteSuccess.LIST_PERIOD_TERMS,
            wrongAnswerNoteService.listPeriodTerms(userId, WrongAnswerPeriod.ALL, page, size, query)
        )

    override fun listGlossaryTerms(
        userId: UUID,
        glossaryId: Long,
        page: Int,
        size: Int,
        query: String?
    ): ApiResponse<WrongAnswerTermsResponse> =
        ApiResponse.success(
            WrongAnswerNoteSuccess.LIST_GLOSSARY_TERMS,
            wrongAnswerNoteService.listGlossaryTerms(userId, glossaryId, page, size, query)
        )

    override fun deleteTerms(
        userId: UUID,
        request: DeleteTermsFromWrongAnswerNoteRequest
    ): ApiResponse<Unit> {
        wrongAnswerNoteService.deleteTerms(userId, request)
        return ApiResponse.success(WrongAnswerNoteSuccess.DELETE_TERMS)
    }
}
