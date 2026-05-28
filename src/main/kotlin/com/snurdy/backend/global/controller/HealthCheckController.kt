package com.snurdy.backend.global.controller

import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.HealthCheckResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

@RestController
class HealthCheckController(
    private val clock: Clock
) : HealthCheckApi {

    override fun healthCheck(): ApiResponse<HealthCheckResponse> {
        val data = HealthCheckResponse(
            status = "UP",
            timestamp = Instant.now(clock)
        )
        return ApiResponse.success(CommonSuccess.OK, data)
    }
}