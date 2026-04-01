package com.snurdy.backend.internal.seed.controller

import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.internal.seed.dto.SeedAnomaliesResponse
import com.snurdy.backend.internal.seed.dto.SeedSummaryResponse
import com.snurdy.backend.internal.seed.service.SeedVerificationService
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@Profile("local", "dev")
@RestController
@RequestMapping("/internal/seed")
class SeedVerificationController(
    private val seedVerificationService: SeedVerificationService
) {

    @GetMapping("/summary")
    fun summary(): ApiResponse<SeedSummaryResponse> =
        ApiResponse.success(CommonSuccess.OK, seedVerificationService.getSummary())

    @GetMapping("/anomalies")
    fun anomalies(): ApiResponse<SeedAnomaliesResponse> =
        ApiResponse.success(CommonSuccess.OK, seedVerificationService.getAnomalies())
}
