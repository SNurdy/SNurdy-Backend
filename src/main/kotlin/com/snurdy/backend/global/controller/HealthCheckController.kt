package com.snurdy.backend.global.controller

import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

@RestController
class HealthCheckController(
    private val clock: Clock
) : HealthCheckApi {

    override fun healthCheck(): ApiResponse<Map<String, Any>> {
        val data = mapOf(
            "status" to "UP",
            "timestamp" to Instant.now(clock).toString()
        )
        return ApiResponse.success(CommonSuccess.OK, data)
    }
}