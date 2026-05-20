package com.snurdy.backend.global.controller

import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "Health", description = "Health Check API")
interface HealthCheckApi {

    @Operation(summary = "서버 헬스체크", description = "서버가 정상 작동 중인지 확인합니다.")
    @ApiSuccessCode("COMMON_S200")
    @SecurityRequirements
    @GetMapping("/v1/health")
    fun healthCheck(): ApiResponse<HealthCheckResponse>
}