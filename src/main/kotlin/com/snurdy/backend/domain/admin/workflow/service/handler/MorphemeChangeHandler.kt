package com.snurdy.backend.domain.admin.workflow.service.handler

import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.*
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestPayloadMapper
import com.snurdy.backend.domain.admin.workflow.service.support.EntityPkParser
import com.snurdy.backend.domain.admin.workflow.service.support.MorphemeVariantGenerator
import com.snurdy.backend.domain.dictionary.morpheme.entity.MorphemeEntity
import com.snurdy.backend.domain.dictionary.morpheme.entity.MorphemeTextLimits
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

@Component
class MorphemeChangeHandler(
    private val morphemeRepository: MorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val variantGenerator: MorphemeVariantGenerator,
    private val payloadMapper: ChangeRequestPayloadMapper
) {

    companion object {
        private val SPELLING_MAX = MorphemeTextLimits.SPELLING_MAX
        private val MEANING_MAX = MorphemeTextLimits.MEANING_MAX
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
                val morphemeId = EntityPkParser.parseForValidation(cr.entityPk, EntityPkParser.LABEL_MORPHEME, v)
                    ?: return v.build()
                if (!morphemeRepository.existsById(morphemeId)) v.error(ValidationCode.MORPHEME_NOT_FOUND, morphemeId)
                validateFields(payload, v)
                checkSpellingDuplicate(payload.spelling, excludeId = morphemeId, v)
            }

            ChangeRequestOperation.DELETE ->
                v.error(ValidationCode.UNSUPPORTED_OPERATION, "어근어미", "CREATE, UPDATE")
        }

        return v.build()
    }

    fun apply(cr: ChangeRequestEntity): String? = when (cr.operation) {
        ChangeRequestOperation.CREATE -> {
            val payload = parsePayload(cr.afterJson)
                ?: throw ApiException(AdminError.WORKFLOW_VALIDATION_FAILED)
            val morpheme = morphemeRepository.save(
                MorphemeEntity(spelling = payload.spelling.trim(), meaning = payload.meaning.trim())
            )
            variantGenerator.regenerate(morpheme)
            morpheme.id?.toString() ?: throw ApiException(AdminError.MORPHEME_NOT_FOUND)
        }

        ChangeRequestOperation.UPDATE -> {
            val morphemeId = EntityPkParser.parseForApply(cr.entityPk)
            val payload = parsePayload(cr.afterJson)
                ?: throw ApiException(AdminError.WORKFLOW_VALIDATION_FAILED)
            val morpheme = morphemeRepository.findById(morphemeId)
                .orElseThrow { ApiException(AdminError.MORPHEME_NOT_FOUND) }
            morpheme.spelling = payload.spelling.trim()
            morpheme.meaning = payload.meaning.trim()
            morphemeRepository.save(morpheme)
            variantGenerator.regenerate(morpheme)
            null
        }

        ChangeRequestOperation.DELETE -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
    }

    fun rollback(cr: ChangeRequestEntity) {
        when (cr.operation) {
            ChangeRequestOperation.CREATE -> {
                val morphemeId = EntityPkParser.parseForRollback(cr.entityPk)
                morphemeVariantRepository.deleteByMorphemeId(morphemeId)
                val morpheme = morphemeRepository.findById(morphemeId)
                    .orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
                morphemeRepository.delete(morpheme)
            }

            ChangeRequestOperation.UPDATE -> {
                val morphemeId = EntityPkParser.parseForRollback(cr.entityPk)
                val beforePayload = parsePayload(cr.beforeJson)
                    ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)
                val morpheme = morphemeRepository.findById(morphemeId)
                    .orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
                morpheme.spelling = beforePayload.spelling.trim()
                morpheme.meaning = beforePayload.meaning.trim()
                morphemeRepository.save(morpheme)
                variantGenerator.regenerate(morpheme)
            }

            ChangeRequestOperation.DELETE -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun validateFields(payload: MorphemePayload, v: ValidationCollector) {
        if (payload.spelling.isBlank()) v.error(ValidationCode.SPELLING_REQUIRED)
        if (payload.spelling.length > SPELLING_MAX) v.error(ValidationCode.SPELLING_TOO_LONG, SPELLING_MAX)
        if (payload.meaning.isBlank()) v.error(ValidationCode.MEANING_REQUIRED)
        if (payload.meaning.length > MEANING_MAX) v.error(ValidationCode.MEANING_TOO_LONG, MEANING_MAX)
    }

    private fun checkSpellingDuplicate(spelling: String, excludeId: Long?, v: ValidationCollector) {
        val duplicates = morphemeRepository.findAllBySpellingIgnoreCase(spelling)
        val duplicate = duplicates.firstOrNull { it.id != excludeId }
        if (duplicate != null) {
            v.warn(ValidationCode.MORPHEME_SPELLING_DUPLICATE, duplicate.spelling, duplicate.id, duplicate.meaning)
        }
    }

    internal fun parsePayload(json: String?): MorphemePayload? = payloadMapper.readMorphemeOrNull(json)
}
