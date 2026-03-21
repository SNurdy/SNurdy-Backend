package com.snurdy.backend.domain.user.controller

import com.snurdy.backend.domain.learning.dto.UserStreakResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "User", description = "사용자 API")
interface UserMeApi {

    @Operation(summary = "내 스트릭 조회")
    @GetMapping("/v1/users/me/streak")
    fun getMyStreak(): ApiResponse<UserStreakResponse>
}
