package com.snurdy.backend.global.annotation

import com.snurdy.backend.global.dto.response.ErrorResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    SwaggerApiResponse(
        responseCode = "401",
        description = "인증 실패 (유효하지 않은 토큰/자격증명)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))]
    ),
    SwaggerApiResponse(
        responseCode = "502",
        description = "외부 API 연동 오류 (구글/애플/카카오 서비스 장애)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))]
    )
)
annotation class AuthErrorResponse