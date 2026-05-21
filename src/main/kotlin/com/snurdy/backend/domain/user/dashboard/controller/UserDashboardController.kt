package com.snurdy.backend.domain.user.dashboard.controller

import com.snurdy.backend.domain.user.dashboard.dto.UserDashboardResponse
import com.snurdy.backend.domain.user.dashboard.service.UserDashboardService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class UserDashboardController(
    private val userDashboardService: UserDashboardService
) : UserDashboardApi {

    override fun getDashboard(
        userId: UUID,
        from: LocalDate?,
        to: LocalDate?
    ): ApiResponse<UserDashboardResponse> =
        ApiResponse.success(
            UserSuccess.GET_DASHBOARD,
            userDashboardService.getDashboard(userId, from, to)
        )
}
