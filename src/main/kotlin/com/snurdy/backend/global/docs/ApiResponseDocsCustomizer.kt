package com.snurdy.backend.global.docs

import com.snurdy.backend.global.constants.ErrorCode
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

// endpoint 의 @ApiSuccessCode, @ApiErrorCodes 어노테이션을 읽어
// Swagger 응답 example 을 endpoint 별로 정확히 생성한다
// 명시 안 된 endpoint 는 OpenApiErrorResponseCustomizer 의 default 응답만 표시된다
@Component
class ApiResponseDocsCustomizer : OperationCustomizer {

    override fun customize(operation: io.swagger.v3.oas.models.Operation, handlerMethod: HandlerMethod): io.swagger.v3.oas.models.Operation {
        val responses = operation.responses ?: return operation

        // 1. success code 가 명시되어 있으면 200 응답에 example 덮어쓰기
        handlerMethod.getMethodAnnotation(ApiSuccessCode::class.java)?.let { ann ->
            val success = ApiCodeRegistry.SUCCESSES[ann.code]
                ?: error("Unknown success code in @ApiSuccessCode: ${ann.code}")
            val statusKey = success.status.value().toString()
            val existing = responses[statusKey] ?: ApiResponse().also { responses[statusKey] = it }
            existing.description = success.message
            val schema = existing.content?.values?.firstOrNull()?.schema
            val content = buildContent(success.code, success.message, schema, success = true)
            existing.content = content
        }

        // 2. 각 error code 마다 응답 추가 또는 example 보강
        handlerMethod.getMethodAnnotation(ApiErrorCodes::class.java)?.let { ann ->
            ann.codes.forEach { code ->
                val error = ApiCodeRegistry.ERRORS[code]
                    ?: error("Unknown error code in @ApiErrorCodes: $code")
                appendErrorExample(responses, error)
            }
        }

        return operation
    }

    private fun appendErrorExample(
        responses: io.swagger.v3.oas.models.responses.ApiResponses,
        error: ErrorCode
    ) {
        val statusKey = error.status.value().toString()
        val response = responses[statusKey] ?: ApiResponse()
            .description(error.message)
            .also { responses[statusKey] = it }

        val mediaType = response.content?.get(JSON) ?: MediaType().also {
            val content = response.content ?: Content().also { c -> response.content = c }
            content.addMediaType(JSON, it)
        }

        if (mediaType.schema == null) {
            mediaType.schema = errorSchema()
        }

        val examples = mediaType.examples ?: linkedMapOf<String, Example>().also { mediaType.examples = it }
        if (!examples.containsKey(error.code)) {
            examples[error.code] = Example()
                .summary(error.code)
                .value(buildErrorBody(error))
        }
    }

    private fun buildContent(code: String, message: String, schema: Schema<*>?, success: Boolean): Content {
        val content = Content()
        val mediaType = MediaType()
        if (schema != null) mediaType.schema = schema
        val examples = linkedMapOf<String, Example>()
        examples[code] = Example()
            .summary(code)
            .value(if (success) buildSuccessBody(code, message) else mapOf<String, Any?>())
        mediaType.examples = examples
        content.addMediaType(JSON, mediaType)
        return content
    }

    private fun buildSuccessBody(code: String, message: String): Map<String, Any?> = linkedMapOf(
        "success" to true,
        "status" to 200,
        "code" to code,
        "message" to message,
        "data" to null,
        "timestamp" to "2026-05-20T00:00:00Z"
    )

    private fun buildErrorBody(error: ErrorCode): Map<String, Any?> = linkedMapOf(
        "success" to false,
        "status" to error.status.value(),
        "code" to error.code,
        "message" to error.message,
        "timestamp" to "2026-05-20T00:00:00Z"
    )

    private fun errorSchema(): Schema<*> = Schema<Any>()
        .type("object")
        .addProperty("success", Schema<Boolean>().type("boolean").example(false))
        .addProperty("status", Schema<Int>().type("integer"))
        .addProperty("code", Schema<String>().type("string"))
        .addProperty("message", Schema<String>().type("string"))
        .addProperty("timestamp", Schema<String>().type("string").format("date-time"))

    companion object {
        private const val JSON = "application/json"
    }
}
