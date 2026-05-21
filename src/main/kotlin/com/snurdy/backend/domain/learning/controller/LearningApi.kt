package com.snurdy.backend.domain.learning.controller

import com.snurdy.backend.domain.learning.dto.LearningSubmitRequest
import com.snurdy.backend.domain.learning.dto.LearningSubmitResponse
import com.snurdy.backend.domain.learning.dto.ReviewDueTermsResponse
import com.snurdy.backend.domain.learning.dto.StudyNextTermsResponse
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Validated
@Tag(name = "Learning", description = "학습/복습 API")
interface LearningApi {

    @Operation(summary = "학습 카드 시작")
    @ApiSuccessCode("LEARNING_S010")
    @ApiErrorCodes("DICTIONARY_E001")
    @GetMapping("/v1/learning/glossaries/{glossaryId}/next-terms")
    fun getNextTerms(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<StudyNextTermsResponse>

    @Operation(summary = "복습 대상 조회")
    @ApiSuccessCode("LEARNING_S011")
    @ApiErrorCodes("DICTIONARY_E001")
    @GetMapping("/v1/learning/glossaries/{glossaryId}/review-terms")
    fun getDueTerms(
        @CurrentUserId userId: UUID,
        @PathVariable glossaryId: Long,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<ReviewDueTermsResponse>

    @Operation(summary = "학습 결과 반영")
    @ApiSuccessCode("LEARNING_S012")
    @ApiErrorCodes("USER_E010", "DICTIONARY_E001", "DICTIONARY_E002", "DICTIONARY_E004", "LEARNING_E001")
    @PostMapping("/v1/learning/submit")
    fun submitLearning(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: LearningSubmitRequest
    ): ApiResponse<LearningSubmitResponse>
}
