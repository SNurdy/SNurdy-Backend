package com.snurdy.backend.global.controller

import com.snurdy.backend.global.constants.SuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
class HealthCheckController : HealthCheckApi {

    override fun healthCheck(): ApiResponse<Map<String, Any>> {
        val data = mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now().format(FORMATTER)
        )
        return ApiResponse.success(SuccessCode.SEARCH_SUCCESS, data)
    }

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}