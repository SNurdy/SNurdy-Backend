package com.snurdy.backend.domain.dashboard.study.controller

import com.snurdy.backend.domain.dashboard.study.dto.StudyDashboardResponse
import com.snurdy.backend.domain.dashboard.study.service.StudyDashboardService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StudyDashboardController(
    private val studyDashboardService: StudyDashboardService
) : StudyDashboardApi {

    override fun getStudyDashboard(userId: UUID): ApiResponse<StudyDashboardResponse> =
        ApiResponse.success(
            UserSuccess.GET_STUDY_DASHBOARD,
            studyDashboardService.getDashboard(userId)
        )
}
