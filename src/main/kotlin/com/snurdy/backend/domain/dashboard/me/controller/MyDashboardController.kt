package com.snurdy.backend.domain.dashboard.me.controller

import com.snurdy.backend.domain.dashboard.me.dto.DashboardGlossaryListResponse
import com.snurdy.backend.domain.dashboard.me.dto.DashboardGlossaryPeriod
import com.snurdy.backend.domain.dashboard.me.dto.DashboardGlossaryStatisticsResponse
import com.snurdy.backend.domain.dashboard.me.dto.MyDashboardResponse
import com.snurdy.backend.domain.dashboard.me.service.MyDashboardService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class MyDashboardController(
    private val myDashboardService: MyDashboardService
) : MyDashboardApi {

    override fun getDashboard(
        userId: UUID,
        startDate: LocalDate?
    ): ApiResponse<MyDashboardResponse> =
        ApiResponse.success(
            UserSuccess.GET_DASHBOARD,
            myDashboardService.getDashboard(userId, startDate)
        )

    override fun listDashboardGlossaries(
        userId: UUID
    ): ApiResponse<DashboardGlossaryListResponse> =
        ApiResponse.success(
            UserSuccess.LIST_DASHBOARD_GLOSSARIES,
            myDashboardService.listGlossaries(userId)
        )

    override fun listDashboardGlossaryStatistics(
        userId: UUID,
        period: DashboardGlossaryPeriod
    ): ApiResponse<DashboardGlossaryStatisticsResponse> =
        ApiResponse.success(
            UserSuccess.LIST_DASHBOARD_GLOSSARY_STATISTICS,
            myDashboardService.listGlossaryStatistics(userId, period)
        )
}
