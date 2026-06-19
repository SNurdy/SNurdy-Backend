// Responsibility: 형태소(Morpheme) 변경 요청의 유효성 검사
package com.snurdy.backend.domain.admin.workflow.service.handler

import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.*
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestPayloadMapper
import com.snurdy.backend.domain.admin.workflow.service.support.EntityPkParser
import com.snurdy.backend.domain.dictionary.morpheme.entity.MorphemeTextLimits
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import org.springframework.stereotype.Component

@Component
class MorphemeValidator(
    private val morphemeRepository: MorphemeRepository,
    private val payloadMapper: ChangeRequestPayloadMapper
) {
    companion object {
        private val SPELLING_MAX = MorphemeTextLimits.SPELLING_MAX
        private val MEANING_MAX = MorphemeTextLimits.MEANING_MAX
    }

    fun validate(cr: ChangeRequestEntity): ValidationResult {
        val v = ValidationCollector()
        when (cr.operation) {
            ChangeRequestOperation.CREATE -> validateCreate(cr, v)
            ChangeRequestOperation.UPDATE -> validateUpdate(cr, v)
            ChangeRequestOperation.DELETE -> v.error(ValidationCode.UNSUPPORTED_OPERATION, "어근어미", "CREATE, UPDATE")
        }
        return v.build()
    }

    private fun validateCreate(cr: ChangeRequestEntity, v: ValidationCollector) {
        val payload = payloadMapper.readMorphemeOrNull(cr.afterJson) ?: run { v.error(ValidationCode.EMPTY_PAYLOAD); return }
        validateFields(payload, v)
        checkSpellingDuplicate(payload.spelling, excludeId = null, v)
    }

    private fun validateUpdate(cr: ChangeRequestEntity, v: ValidationCollector) {
        val payload = payloadMapper.readMorphemeOrNull(cr.afterJson) ?: run { v.error(ValidationCode.EMPTY_PAYLOAD); return }
        val morphemeId = EntityPkParser.parseForValidation(cr.entityPk, EntityPkParser.LABEL_MORPHEME, v) ?: return
        if (!morphemeRepository.existsById(morphemeId)) v.error(ValidationCode.MORPHEME_NOT_FOUND, morphemeId)
        validateFields(payload, v)
        checkSpellingDuplicate(payload.spelling, excludeId = morphemeId, v)
    }

    private fun validateFields(payload: MorphemePayload, v: ValidationCollector) {
        if (payload.spelling.isBlank()) v.error(ValidationCode.SPELLING_REQUIRED)
        if (payload.spelling.length > SPELLING_MAX) v.error(ValidationCode.SPELLING_TOO_LONG, SPELLING_MAX)
        if (payload.meaning.isBlank()) v.error(ValidationCode.MEANING_REQUIRED)
        if (payload.meaning.length > MEANING_MAX) v.error(ValidationCode.MEANING_TOO_LONG, MEANING_MAX)
    }

    private fun checkSpellingDuplicate(spelling: String, excludeId: Long?, v: ValidationCollector) {
        morphemeRepository.findAllBySpellingIgnoreCase(spelling)
            .firstOrNull { it.id != excludeId }
            ?.let { v.warn(ValidationCode.MORPHEME_SPELLING_DUPLICATE, it.spelling, it.id, it.meaning) }
    }
}
