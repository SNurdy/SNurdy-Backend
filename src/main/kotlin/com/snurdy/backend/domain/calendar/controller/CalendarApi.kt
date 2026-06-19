package com.snurdy.backend.domain.calendar.controller

import com.snurdy.backend.domain.calendar.dto.CalendarResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Validated
@Tag(name = "Calendar", description = "학습 캘린더 API")
interface CalendarApi {

    @Operation(summary = "월별 학습 캘린더 조회")
    @ApiSuccessCode("CALENDAR_S001")
    @ApiErrorCodes("USER_E010")
    @GetMapping("/v1/calendar")
    fun getMonthly(
        @CurrentUserId userId: UUID,
        @RequestParam @Min(2000) @Max(2100) year: Int,
        @RequestParam @Min(1) @Max(12) month: Int
    ): ApiResponse<CalendarResponse>
}
