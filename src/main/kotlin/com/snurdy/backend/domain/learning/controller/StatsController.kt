package com.snurdy.backend.domain.learning.controller

import com.snurdy.backend.domain.learning.dto.CalendarStatsResponse
import com.snurdy.backend.domain.learning.service.LearningService
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.extension.currentUserId
import org.springframework.web.bind.annotation.RestController

@RestController
class StatsController(
    private val learningService: LearningService
) : StatsApi {
    override fun getCalendar(year: Int, month: Int): ApiResponse<CalendarStatsResponse> =
        ApiResponse.success(
            CommonSuccess.OK,
            learningService.getCalendar(currentUserId(), year, month)
        )
}
