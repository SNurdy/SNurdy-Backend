package com.snurdy.backend.global.controller

import com.snurdy.backend.global.common.constants.SuccessCode
import com.snurdy.backend.global.common.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Tag(name = "HealthCheck", description = "HealthCheck API")
@RestController
class HealthCheckController {

    @Operation(summary = "서버 헬스체크", description = "서버가 정상 작동 중인지 확인합니다.")
    @GetMapping("/v1/health")
    fun healthCheck(): ApiResponse<Map<String, Any>> {
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