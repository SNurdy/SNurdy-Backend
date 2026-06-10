package com.snurdy.backend.domain.dashboard.me.controller

import com.snurdy.backend.domain.dashboard.me.dto.DashboardGlossaryListResponse
import com.snurdy.backend.domain.dashboard.me.dto.DashboardGlossaryPeriod
import com.snurdy.backend.domain.dashboard.me.dto.DashboardGlossaryStatisticsResponse
import com.snurdy.backend.domain.dashboard.me.dto.MyDashboardResponse
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

@Tag(name = "My Dashboard", description = "마이페이지 탭 대시보드 API")
@RequestMapping("/v2/dashboards/me")
interface MyDashboardApi {

    @Operation(summary = "마이페이지 메인 대시보드 (streak + 선택된 과 + 7일치 막대 그래프)")
    @ApiSuccessCode("USER_S020")
    @ApiErrorCodes("USER_E010")
    @GetMapping
    fun getDashboard(
        @CurrentUserId userId: UUID,
        @Parameter(description = "조회 시작일 (yyyy-MM-dd). 미지정 시 오늘 포함 직전 7일, 지정 시 startDate~startDate+6")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate?
    ): ApiResponse<MyDashboardResponse>

    @Operation(summary = "과 변경 바텀시트 목록 (전체 27개 과 + 사용자 진척률)")
    @ApiSuccessCode("USER_S021")
    @ApiErrorCodes("USER_E010")
    @GetMapping("/glossaries")
    fun listDashboardGlossaries(
        @CurrentUserId userId: UUID
    ): ApiResponse<DashboardGlossaryListResponse>

    @Operation(summary = "과 지표 리스트 (학습 시작한 과만, period 별 학습 단어 수 + 오답 노트 수)")
    @ApiSuccessCode("USER_S022")
    @ApiErrorCodes("USER_E010")
    @GetMapping("/glossary-statistics")
    fun listDashboardGlossaryStatistics(
        @CurrentUserId userId: UUID,
        @Parameter(description = "조회 기간 (WEEK 이번 주 자연주, ALL 전체)")
        @RequestParam(defaultValue = "WEEK") period: DashboardGlossaryPeriod
    ): ApiResponse<DashboardGlossaryStatisticsResponse>
}
