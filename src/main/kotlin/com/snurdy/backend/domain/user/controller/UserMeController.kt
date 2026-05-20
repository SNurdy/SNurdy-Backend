package com.snurdy.backend.domain.user.controller

import com.snurdy.backend.domain.user.dto.UserStreakResponse
import com.snurdy.backend.domain.user.service.UserStreakService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserMeController(
    private val userStreakService: UserStreakService
) : UserMeApi {
    override fun getMyStreak(userId: UUID): ApiResponse<UserStreakResponse> =
        ApiResponse.success(UserSuccess.GET_STREAK, userStreakService.getStreak(userId))
}
