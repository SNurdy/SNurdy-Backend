package com.snurdy.backend.domain.user.streak.controller

import com.snurdy.backend.domain.user.streak.dto.UserStreakResponse
import com.snurdy.backend.domain.user.streak.service.UserStreakService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserStreakController(
    private val userStreakService: UserStreakService
) : UserStreakApi {
    override fun getMyStreak(userId: UUID): ApiResponse<UserStreakResponse> =
        ApiResponse.success(UserSuccess.GET_STREAK, userStreakService.getStreak(userId))
}
