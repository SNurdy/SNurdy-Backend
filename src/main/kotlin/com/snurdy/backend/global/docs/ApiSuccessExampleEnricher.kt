package com.snurdy.backend.global.docs

import com.snurdy.backend.global.constants.SuccessCode
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

// ApiResponseDocsCustomizer 가 남긴 x-snurdy-success-code 마커를 읽어
// 응답 schema 트리를 재귀로 풀어 data 필드까지 포함한 example JSON 을 만들어 부착한다
// $ref 가 해석된 시점에 실행되어야 하므로 OpenApiCustomizer 로 구현했다
@Component
class ApiSuccessExampleEnricher : OpenApiCustomizer {

    override fun customise(openApi: OpenAPI) {
        val components = openApi.components ?: return
        openApi.paths?.values?.forEach { pathItem ->
            pathItem.readOperations().forEach { operation ->
                val code = operation.extensions?.get(ApiResponseDocsCustomizer.EXT_SUCCESS_CODE) as? String
                    ?: return@forEach
                val success = ApiCodeRegistry.SUCCESSES[code] ?: return@forEach
                enrich(operation, success, components)
                operation.extensions?.remove(ApiResponseDocsCustomizer.EXT_SUCCESS_CODE)
                if (operation.extensions?.isEmpty() == true) operation.extensions = null
            }
        }
    }

    private fun enrich(
        operation: io.swagger.v3.oas.models.Operation,
        success: SuccessCode,
        components: Components
    ) {
        val statusKey = success.status.value().toString()
        val response = operation.responses?.get(statusKey) ?: return
        val mediaType = response.content?.get(JSON)
            ?: response.content?.values?.firstOrNull()
            ?: return
        val schema = mediaType.schema ?: return

        @Suppress("UNCHECKED_CAST")
        val body = (generateExample(schema, components) as? Map<String, Any?>)
            ?.toMutableMap()
            ?: linkedMapOf()
        body["success"] = true
        body["status"] = success.status.value()
        body["code"] = success.code
        body["message"] = success.message
        body.putIfAbsent("timestamp", "2026-05-20T00:00:00Z")

        val examples = mediaType.examples
            ?: linkedMapOf<String, Example>().also { mediaType.examples = it }
        examples[success.code] = Example()
            .summary(success.code)
            .value(body)
        response.description = success.message
    }

    private fun generateExample(
        schema: Schema<*>,
        components: Components,
        seen: MutableSet<String> = mutableSetOf()
    ): Any? {
        val resolved = resolveRef(schema, components, seen) ?: return null

        resolved.example?.let { return it }

        return when (resolved.type) {
            "object", null -> {
                val props = resolved.properties
                if (props.isNullOrEmpty()) return linkedMapOf<String, Any?>()
                val map = linkedMapOf<String, Any?>()
                props.forEach { (k, v) ->
                    map[k] = generateExample(v, components, seen.toMutableSet())
                }
                map
            }
            "array" -> {
                val item = resolved.items?.let { generateExample(it, components, seen.toMutableSet()) }
                listOf(item)
            }
            "string" -> when (resolved.format) {
                "date-time" -> "2026-05-20T00:00:00Z"
                "date" -> "2026-05-20"
                "uuid" -> "00000000-0000-0000-0000-000000000000"
                else -> resolved.enum?.firstOrNull()?.toString() ?: "string"
            }
            "integer" -> 0
            "number" -> 0.0
            "boolean" -> true
            else -> null
        }
    }

    private fun resolveRef(
        schema: Schema<*>,
        components: Components,
        seen: MutableSet<String>
    ): Schema<*>? {
        val ref = schema.`$ref` ?: return schema
        val name = ref.substringAfterLast("/")
        if (!seen.add(name)) return null
        return components.schemas?.get(name)
    }

    companion object {
        private const val JSON = "application/json"
    }
}
