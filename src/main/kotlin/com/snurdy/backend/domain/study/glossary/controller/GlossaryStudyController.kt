package com.snurdy.backend.domain.study.glossary.controller

import com.snurdy.backend.domain.study.glossary.dto.GlossaryStudyCardsRequest
import com.snurdy.backend.domain.study.glossary.dto.GlossaryStudyCardsResponse
import com.snurdy.backend.domain.study.glossary.service.GlossaryStudyService
import com.snurdy.backend.global.constants.StudySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class GlossaryStudyController(
    private val glossaryStudyService: GlossaryStudyService
) : GlossaryStudyApi {

    override fun listStudyCards(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryStudyCardsRequest
    ): ApiResponse<GlossaryStudyCardsResponse> =
        ApiResponse.success(
            StudySuccess.LIST_STUDY_CARDS,
            glossaryStudyService.listStudyCards(userId, glossaryId, request)
        )
}