package com.snurdy.backend.domain.admin.workflow.service.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.*
import com.snurdy.backend.domain.admin.workflow.service.support.MorphemeVariantGenerator
import com.snurdy.backend.domain.catalog.morpheme.entity.MorphemeEntity
import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

@Component
class MorphemeChangeHandler(
    private val morphemeRepository: MorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val variantGenerator: MorphemeVariantGenerator,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val SPELLING_MAX = 100
        private const val MEANING_MAX = 100
    }

    fun validate(cr: ChangeRequestEntity): ValidationResult {
        val v = ValidationCollector()

        when (cr.operation) {
            ChangeRequestOperation.CREATE -> {
                val payload = parsePayload(cr.afterJson)
                if (payload == null) { v.error(ValidationCode.EMPTY_PAYLOAD); return v.build() }
                validateFields(payload, v)
                checkSpellingDuplicate(payload.spelling, excludeId = null, v)
            }

            ChangeRequestOperation.UPDATE -> {
                val payload = parsePayload(cr.afterJson)
                if (payload == null) { v.error(ValidationCode.EMPTY_PAYLOAD); return v.build() }
                val morphemeId = cr.entityPk.toLongOrNull()
                if (morphemeId == null) { v.error(ValidationCode.INVALID_ENTITY_PK, "형태소"); return v.build() }
                if (!morphemeRepository.existsById(morphemeId)) v.error(ValidationCode.MORPHEME_NOT_FOUND, morphemeId)
                validateFields(payload, v)
                checkSpellingDuplicate(payload.spelling, excludeId = morphemeId, v)
            }

            else -> v.error(ValidationCode.UNSUPPORTED_OPERATION, "형태소", "CREATE, UPDATE")
        }

        return v.build()
    }

    fun apply(cr: ChangeRequestEntity): String? {
        return when (cr.operation) {
            ChangeRequestOperation.CREATE -> {
                val payload = parsePayload(cr.afterJson)!!
                val morpheme = morphemeRepository.save(
                    MorphemeEntity(spelling = payload.spelling.trim(), meaning = payload.meaning.trim())
                )
                variantGenerator.regenerate(morpheme)
                morpheme.id.toString()
            }

            ChangeRequestOperation.UPDATE -> {
                val morphemeId = cr.entityPk.toLong()
                val payload = parsePayload(cr.afterJson)!!
                val morpheme = morphemeRepository.findById(morphemeId)
                    .orElseThrow { ApiException(AdminError.MORPHEME_NOT_FOUND) }
                morpheme.spelling = payload.spelling.trim()
                morpheme.meaning = payload.meaning.trim()
                morphemeRepository.save(morpheme)
                variantGenerator.regenerate(morpheme)
                null
            }

            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    fun rollback(cr: ChangeRequestEntity) {
        when (cr.operation) {
            ChangeRequestOperation.CREATE -> {
                val morphemeId = cr.entityPk.toLongOrNull()
                    ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)
                morphemeVariantRepository.deleteByMorphemeId(morphemeId)
                val morpheme = morphemeRepository.findById(morphemeId)
                    .orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
                morphemeRepository.delete(morpheme)
            }

            ChangeRequestOperation.UPDATE -> {
                // UPDATE 롤백 = beforeJson으로 복원
                val morphemeId = cr.entityPk.toLong()
                val beforePayload = parsePayload(cr.beforeJson)
                    ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)
                val morpheme = morphemeRepository.findById(morphemeId)
                    .orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
                morpheme.spelling = beforePayload.spelling.trim()
                morpheme.meaning = beforePayload.meaning.trim()
                morphemeRepository.save(morpheme)
                variantGenerator.regenerate(morpheme)
            }

            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun validateFields(payload: MorphemePayload, v: ValidationCollector) {
        if (payload.spelling.isBlank()) v.error(ValidationCode.SPELLING_REQUIRED)
        if (payload.spelling.length > SPELLING_MAX) v.error(ValidationCode.SPELLING_TOO_LONG, SPELLING_MAX)
        if (payload.meaning.isBlank()) v.error(ValidationCode.MEANING_REQUIRED)
        if (payload.meaning.length > MEANING_MAX) v.error(ValidationCode.MEANING_TOO_LONG, MEANING_MAX)
    }

    private fun checkSpellingDuplicate(spelling: String, excludeId: Long?, v: ValidationCollector) {
        val duplicate = morphemeRepository.findBySpellingIgnoreCase(spelling)
        if (duplicate != null && duplicate.id != excludeId) {
            v.warn(ValidationCode.MORPHEME_SPELLING_DUPLICATE, duplicate.spelling, duplicate.id, duplicate.meaning)
        }
    }

    internal fun parsePayload(json: String?): MorphemePayload? =
        json?.takeIf { it.isNotBlank() }?.let { objectMapper.readValue(it) }
}