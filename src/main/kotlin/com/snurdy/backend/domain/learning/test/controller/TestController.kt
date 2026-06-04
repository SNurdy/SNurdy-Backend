package com.snurdy.backend.domain.learning.test.controller

import com.snurdy.backend.domain.learning.test.dto.StartTestRequest
import com.snurdy.backend.domain.learning.test.dto.StartTestResponse
import com.snurdy.backend.domain.learning.test.service.TestService
import com.snurdy.backend.global.constants.LearningSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class TestController(
    private val testService: TestService
) : TestApi {

    override fun startTest(
        userId: UUID,
        request: StartTestRequest
    ): ApiResponse<StartTestResponse> =
        ApiResponse.success(
            LearningSuccess.START_TEST,
            testService.startTest(userId, request)
        )
}
