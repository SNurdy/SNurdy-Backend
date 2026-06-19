package com.snurdy.backend.global.docs

import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    init {
        // @CurrentUserId 는 토큰에서 자동 주입되므로 Swagger 문서의 파라미터 목록에서 제외한다
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUserId::class.java)
    }

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
}