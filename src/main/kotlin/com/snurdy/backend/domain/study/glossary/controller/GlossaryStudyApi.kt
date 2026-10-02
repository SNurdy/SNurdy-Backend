package com.snurdy.backend.domain.study.glossary.controller

import com.snurdy.backend.domain.study.glossary.docs.GlossaryStudySwaggerDescription
import com.snurdy.backend.domain.study.glossary.dto.GlossaryStudyCardsRequest
import com.snurdy.backend.domain.study.glossary.dto.GlossaryStudyCardsResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(
    name = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_API_TAG_NAME,
    description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_API_TAG_DESCRIPTION
)
@RequestMapping("/v2/study/glossaries/{glossaryId}")
interface GlossaryStudyApi {

    @Operation(summary = GlossaryStudySwaggerDescription.LIST_STUDY_CARDS_SUMMARY)
    @ApiSuccessCode("STUDY_S001")
    @ApiErrorCodes("DICTIONARY_E001")
    @PostMapping("/study-cards")
    fun listStudyCards(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestBody @Valid request: GlossaryStudyCardsRequest
    ): ApiResponse<GlossaryStudyCardsResponse>
}