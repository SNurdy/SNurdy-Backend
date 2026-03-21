package com.snurdy.backend.domain.learning.controller

import com.snurdy.backend.domain.learning.dto.CalendarStatsResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Validated
@Tag(name = "Stats", description = "학습 통계 API")
interface StatsApi {

    @Operation(summary = "월별 캘린더 통계 조회")
    @GetMapping("/v1/stats/calendar")
    fun getCalendar(
        @RequestParam @Min(2000) @Max(2100) year: Int,
        @RequestParam @Min(1) @Max(12) month: Int
    ): ApiResponse<CalendarStatsResponse>
}
