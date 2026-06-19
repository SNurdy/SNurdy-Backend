package com.snurdy.backend.domain.dashboard.study.controller

import com.snurdy.backend.domain.dashboard.study.dto.StudyDashboardResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(name = "Study Dashboard", description = "학습하기 탭 대시보드 API")
@RequestMapping("/v2/dashboards/study")
interface StudyDashboardApi {

    @Operation(summary = "학습하기 탭 메인 대시보드")
    @ApiSuccessCode("USER_S023")
    @ApiErrorCodes("USER_E010", "DICTIONARY_E001")
    @GetMapping
    fun getStudyDashboard(@CurrentUserId userId: UUID): ApiResponse<StudyDashboardResponse>
}
