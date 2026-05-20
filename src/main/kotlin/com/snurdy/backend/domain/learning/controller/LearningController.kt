package com.snurdy.backend.domain.learning.controller

import com.snurdy.backend.domain.learning.dto.LearningSubmitRequest
import com.snurdy.backend.domain.learning.dto.LearningSubmitResponse
import com.snurdy.backend.domain.learning.dto.ReviewDueTermsResponse
import com.snurdy.backend.domain.learning.dto.StudyNextTermsResponse
import com.snurdy.backend.domain.learning.service.LearningCommandService
import com.snurdy.backend.domain.learning.service.LearningQueryService
import com.snurdy.backend.global.constants.LearningSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class LearningController(
    private val learningQueryService: LearningQueryService,
    private val learningCommandService: LearningCommandService
) : LearningApi {
    override fun getNextTerms(userId: UUID, glossaryId: Long, size: Int): ApiResponse<StudyNextTermsResponse> =
        ApiResponse.success(
            LearningSuccess.GET_NEXT_TERMS,
            learningQueryService.getNextTerms(userId, glossaryId, size)
        )

    override fun getDueTerms(userId: UUID, glossaryId: Long, size: Int): ApiResponse<ReviewDueTermsResponse> =
        ApiResponse.success(
            LearningSuccess.GET_REVIEW_TERMS,
            learningQueryService.getDueTerms(userId, glossaryId, size)
        )

    override fun submitLearning(userId: UUID, request: LearningSubmitRequest): ApiResponse<LearningSubmitResponse> =
        ApiResponse.success(
            LearningSuccess.SUBMIT,
            learningCommandService.submitLearning(userId, request)
        )
}
