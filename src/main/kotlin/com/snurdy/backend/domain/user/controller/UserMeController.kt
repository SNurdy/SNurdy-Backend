package com.snurdy.backend.domain.user.controller

import com.snurdy.backend.domain.learning.dto.UserStreakResponse
import com.snurdy.backend.domain.learning.service.LearningQueryService
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserMeController(
    private val learningQueryService: LearningQueryService
) : UserMeApi {
    override fun getMyStreak(userId: UUID): ApiResponse<UserStreakResponse> =
        ApiResponse.success(CommonSuccess.OK, learningQueryService.getStreak(userId))
}
