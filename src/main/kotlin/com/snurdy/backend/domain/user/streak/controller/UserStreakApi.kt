package com.snurdy.backend.domain.user.streak.controller

import com.snurdy.backend.domain.user.streak.dto.UserStreakResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import java.util.UUID

@Tag(name = "User Streak", description = "사용자 학습 streak API")
interface UserStreakApi {

    @Operation(summary = "내 스트릭 조회")
    @ApiSuccessCode("USER_S010")
    @ApiErrorCodes("USER_E010")
    @GetMapping("/v1/users/me/streak")
    fun getMyStreak(@CurrentUserId userId: UUID): ApiResponse<UserStreakResponse>
}
