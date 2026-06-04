package com.snurdy.backend.domain.user.dashboard.controller

import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryDetailResponse
import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryListResponse
import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryPeriod
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
        startDate: LocalDate?
    ): ApiResponse<UserDashboardResponse> =
        ApiResponse.success(
            UserSuccess.GET_DASHBOARD,
            userDashboardService.getDashboard(userId, startDate)
        )

    override fun listDashboardGlossaries(
        userId: UUID
    ): ApiResponse<DashboardGlossaryListResponse> =
        ApiResponse.success(
            UserSuccess.LIST_DASHBOARD_GLOSSARIES,
            userDashboardService.listGlossaries(userId)
        )

    override fun getDashboardGlossaryDetail(
        userId: UUID,
        glossaryId: Long,
        period: DashboardGlossaryPeriod
    ): ApiResponse<DashboardGlossaryDetailResponse> =
        ApiResponse.success(
            UserSuccess.GET_DASHBOARD_GLOSSARY_DETAIL,
            userDashboardService.getGlossaryDetail(userId, glossaryId, period)
        )
}
