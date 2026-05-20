package com.snurdy.backend.domain.learning.controller

import com.snurdy.backend.domain.learning.dto.CalendarStatsResponse
import com.snurdy.backend.domain.learning.service.LearningQueryService
import com.snurdy.backend.global.constants.StatsSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StatsController(
    private val learningQueryService: LearningQueryService
) : StatsApi {
    override fun getCalendar(userId: UUID, year: Int, month: Int): ApiResponse<CalendarStatsResponse> =
        ApiResponse.success(
            StatsSuccess.CALENDAR,
            learningQueryService.getCalendar(userId, year, month)
        )
}
