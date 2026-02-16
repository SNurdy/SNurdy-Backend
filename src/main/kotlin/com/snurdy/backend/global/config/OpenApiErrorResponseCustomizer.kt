package com.snurdy.backend.global.config

import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.ErrorCode
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse as OasApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class OpenApiErrorResponseCustomizer : OpenApiCustomizer {

    override fun customise(openApi: io.swagger.v3.oas.models.OpenAPI) {
        val components = openApi.components ?: Components().also { openApi.components = it }

        registerErrorSchemas(components)
        addErrorResponsesToAllOperations(openApi)
    }

    private fun registerErrorSchemas(components: Components) {
        ERROR_RESPONSES.forEach { spec ->
            if (components.schemas?.containsKey(spec.schemaName) != true) {
                components.addSchemas(spec.schemaName, spec.toSchema())
            }
        }
    }

    private fun addErrorResponsesToAllOperations(openApi: io.swagger.v3.oas.models.OpenAPI) {
        openApi.paths?.values?.forEach { pathItem ->
            pathItem.readOperations().forEach { operation ->
                val responses = operation.responses
                ERROR_RESPONSES.forEach { spec ->
                    responses.putIfAbsent(
                        spec.errorCode.status.value().toString(),
                        createErrorResponse(spec.description, spec.schemaName)
                    )
                }
            }
        }
    }

    private fun createErrorResponse(description: String, schemaRef: String): OasApiResponse =
        OasApiResponse()
            .description(description)
            .content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/$schemaRef"))
                )
            )

    companion object {
        private val ERROR_RESPONSES = listOf(
            ErrorResponseSpec("ErrorResponse400", "잘못된 요청", CommonError.BAD_REQUEST),
            ErrorResponseSpec("ErrorResponse401", "인증 실패", AuthError.INVALID_TOKEN),
            ErrorResponseSpec("ErrorResponse500", "서버 오류", CommonError.INTERNAL_ERROR),
            ErrorResponseSpec("ErrorResponse502", "외부 API 오류", AuthError.KAKAO_API_ERROR),
        )
    }

    private data class ErrorResponseSpec(
        val schemaName: String,
        val description: String,
        val errorCode: ErrorCode
    ) {
        fun toSchema(): Schema<*> =
            Schema<Any>()
                .type("object")
                .addProperty("success", Schema<Boolean>().type("boolean").example(false))
                .addProperty("status", Schema<Int>().type("integer").example(errorCode.status.value()))
                .addProperty("code", Schema<String>().type("string").example(errorCode.code))
                .addProperty("message", Schema<String>().type("string").example(errorCode.message))
                .addProperty("timestamp", Schema<String>().type("string").format("date-time"))
    }
}