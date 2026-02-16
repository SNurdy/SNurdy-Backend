package com.snurdy.backend.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse as OasApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("SNurdy API")
                    .description("SNurdy API 문서")
                    .version("v1.0.0")
            )
            .servers(
                listOf(
                    Server()
                        .url("https://api-test.snurdy.org/api")
                        .description("개발 서버"),
                    Server()
                        .url("http://localhost:8080/api")
                        .description("로컬 서버")
                )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "Access Token",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .`in`(SecurityScheme.In.HEADER)
                            .name("Authorization")
                    )
                    .addSecuritySchemes(
                        "Refresh Token",
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name("Authorization-Refresh")
                    )
                    // 에러 응답 스키마 명시적 등록
                    .addSchemas("ErrorResponse", errorResponseSchema())
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList("Access Token")
            )

    @Bean
    fun globalErrorResponseCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        val components = openApi.components ?: Components().also { openApi.components = it }
        if (components.schemas?.containsKey("ErrorResponse") != true) {
            components.addSchemas("ErrorResponse", errorResponseSchema())
        }
        openApi.paths?.values?.forEach { pathItem ->
            pathItem.readOperations().forEach { operation ->
                val responses = operation.responses
                responses.putIfAbsent("400", createErrorResponse("잘못된 요청"))
                responses.putIfAbsent("401", createErrorResponse("인증 실패"))
                responses.putIfAbsent("500", createErrorResponse("서버 오류"))
                responses.putIfAbsent("502", createErrorResponse("외부 API 오류"))
            }
        }
    }

    private fun createErrorResponse(description: String): OasApiResponse =
        OasApiResponse()
            .description(description)
            .content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ErrorResponse"))
                )
            )

    private fun errorResponseSchema(): Schema<*> =
        Schema<Any>()
            .type("object")
            .addProperty("success", Schema<Boolean>().type("boolean").example(false))
            .addProperty("status", Schema<Int>().type("integer").example(400))
            .addProperty("code", Schema<String>().type("string").example("COMMON_400"))
            .addProperty("message", Schema<String>().type("string").example("잘못된 요청입니다"))
            .addProperty("timestamp", Schema<String>().type("string").format("date-time"))
}