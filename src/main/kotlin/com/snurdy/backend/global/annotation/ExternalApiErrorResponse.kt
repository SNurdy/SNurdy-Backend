package com.snurdy.backend.global.annotation

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import com.snurdy.backend.global.dto.response.ApiResponse as AppApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    ApiResponse(
        responseCode = "502",
        description = "외부 API 연동 오류",
        content = [Content(schema = Schema(implementation = AppApiResponse::class))]
    )
)
annotation class ExternalApiErrorResponse