package com.snurdy.backend.domain.user.controller

import com.snurdy.backend.domain.user.dto.UserDashboardResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate
import java.util.UUID

@Tag(name = "User-Dashboard", description = "v2 학습 리포트 대시보드 API")
@RequestMapping("/v2/users/me")
interface UserDashboardApi {

    @Operation(summary = "학습 리포트 대시보드 조회 (streak + 과별 + 일자별 통합)")
    @ApiSuccessCode("USER_S020")
    @ApiErrorCodes("USER_E010")
    @GetMapping("/dashboard")
    fun getDashboard(
        @CurrentUserId userId: UUID,
        @Parameter(description = "조회 시작일 (yyyy-MM-dd). 미지정 시 to 기준 30일 전")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate?,
        @Parameter(description = "조회 종료일 (yyyy-MM-dd). 미지정 시 오늘")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate?
    ): ApiResponse<UserDashboardResponse>
}
