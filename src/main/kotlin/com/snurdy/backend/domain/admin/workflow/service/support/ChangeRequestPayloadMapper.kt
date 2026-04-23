package com.snurdy.backend.domain.admin.workflow.service.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.snurdy.backend.domain.admin.workflow.service.model.MorphemePayload
import com.snurdy.backend.domain.admin.workflow.service.model.TermPayload
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

@Component
class ChangeRequestPayloadMapper(
    private val objectMapper: ObjectMapper
) {
    fun readTermOrNull(json: String?): TermPayload? =
        json?.takeIf { it.isNotBlank() }?.let {
            runCatching { objectMapper.readValue<TermPayload>(it) }
                .getOrElse { throw ApiException(AdminError.WORKFLOW_VALIDATION_FAILED, cause = it) }
        }

    fun readMorphemeOrNull(json: String?): MorphemePayload? =
        json?.takeIf { it.isNotBlank() }?.let {
            runCatching { objectMapper.readValue<MorphemePayload>(it) }
                .getOrElse { throw ApiException(AdminError.WORKFLOW_VALIDATION_FAILED, cause = it) }
        }

    fun readMutableMap(json: String?): MutableMap<String, Any?> =
        json?.takeIf { it.isNotBlank() }?.let {
            runCatching { objectMapper.readValue<MutableMap<String, Any?>>(it) }
                .getOrElse { throw ApiException(AdminError.WORKFLOW_VALIDATION_FAILED, cause = it) }
        } ?: mutableMapOf()

    fun writeAsString(value: Any?): String? =
        value?.let {
            runCatching { objectMapper.writeValueAsString(it) }
                .getOrElse { throw ApiException(CommonError.INTERNAL_ERROR, cause = it) }
        }

    fun writeRequired(value: Any): String =
        writeAsString(value) ?: throw ApiException(CommonError.INTERNAL_ERROR)
}
