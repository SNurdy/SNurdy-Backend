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
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList("Access Token")
            )

    @Bean
    fun globalErrorResponseCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.paths?.values?.forEach { pathItem ->
            pathItem.readOperations().forEach { operation ->
                val responses = operation.responses
                responses.putIfAbsent("400", createErrorResponse("잘못된 요청"))
                responses.putIfAbsent("401", createErrorResponse("인증 실패"))
                responses.putIfAbsent("500", createErrorResponse("서버 오류"))
            }
        }
    }

    private fun createErrorResponse(description: String): OasApiResponse =
        OasApiResponse()
            .description(description)
            .content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ApiResponse"))
                )
            )
}