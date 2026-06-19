package com.snurdy.backend.domain.calendar.controller

import com.snurdy.backend.domain.calendar.dto.CalendarResponse
import com.snurdy.backend.domain.calendar.service.CalendarService
import com.snurdy.backend.global.constants.CalendarSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CalendarController(
    private val calendarService: CalendarService
) : CalendarApi {
    override fun getMonthly(userId: UUID, year: Int, month: Int): ApiResponse<CalendarResponse> =
        ApiResponse.success(
            CalendarSuccess.GET_MONTHLY,
            calendarService.getMonthly(userId, year, month)
        )
}
