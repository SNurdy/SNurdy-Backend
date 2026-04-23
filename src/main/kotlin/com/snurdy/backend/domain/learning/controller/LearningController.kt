package com.snurdy.backend.domain.learning.controller

import com.snurdy.backend.domain.learning.dto.LearningSubmitRequest
import com.snurdy.backend.domain.learning.dto.LearningSubmitResponse
import com.snurdy.backend.domain.learning.dto.ReviewDueTermsResponse
import com.snurdy.backend.domain.learning.dto.StudyNextTermsResponse
import com.snurdy.backend.domain.learning.service.LearningService
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class LearningController(
    private val learningService: LearningService
) : LearningApi {
    override fun getNextTerms(userId: UUID, glossaryId: Long, size: Int): ApiResponse<StudyNextTermsResponse> =
        ApiResponse.success(
            CommonSuccess.OK,
            learningService.getNextTerms(userId, glossaryId, size)
        )

    override fun getDueTerms(userId: UUID, glossaryId: Long, size: Int): ApiResponse<ReviewDueTermsResponse> =
        ApiResponse.success(
            CommonSuccess.OK,
            learningService.getDueTerms(userId, glossaryId, size)
        )

    override fun submitLearning(userId: UUID, request: LearningSubmitRequest): ApiResponse<LearningSubmitResponse> =
        ApiResponse.success(
            CommonSuccess.OK,
            learningService.submitLearning(userId, request)
        )
}
