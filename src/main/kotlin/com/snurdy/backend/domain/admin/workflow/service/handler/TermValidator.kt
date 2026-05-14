// Responsibility: 용어(Term) 변경 요청의 유효성 검사
package com.snurdy.backend.domain.admin.workflow.service.handler

import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.*
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestPayloadMapper
import com.snurdy.backend.domain.admin.workflow.service.support.EntityPkParser
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermTextLimits
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import org.springframework.stereotype.Component

@Component
class TermValidator(
    private val termRepository: TermRepository,
    private val morphemeRepository: MorphemeRepository,
    private val glossaryRepository: GlossaryRepository,
    private val payloadMapper: ChangeRequestPayloadMapper
) {
    companion object {
        private val SPELLING_MAX = TermTextLimits.SPELLING_MAX
        private val MEANING_MAX = TermTextLimits.MEANING_MAX
        private val ABBREVIATION_MAX = TermTextLimits.ABBREVIATION_MAX
    }

    fun validate(cr: ChangeRequestEntity): ValidationResult {
        val v = ValidationCollector()
        when (cr.operation) {
            ChangeRequestOperation.CREATE -> validateCreate(cr, v)
            ChangeRequestOperation.UPDATE -> validateUpdate(cr, v)
            ChangeRequestOperation.DELETE -> validateDelete(cr, v)
        }
        return v.build()
    }

    private fun validateCreate(cr: ChangeRequestEntity, v: ValidationCollector) {
        val payload = payloadMapper.readTermOrNull(cr.afterJson) ?: run { v.error(ValidationCode.EMPTY_PAYLOAD); return }
        validateFields(payload, v)

        val glossaryIds = resolveGlossaryIds(payload) ?: run { v.error(ValidationCode.GLOSSARY_REQUIRED); return }
        if (glossaryIds.isEmpty()) { v.error(ValidationCode.EMPTY_GLOSSARY_IDS); return }

        validateGlossaryIds(glossaryIds, v)
        validateMorphemeIds(payload.morphemes, v)
        checkSpellingDuplicate(payload.spelling, excludeId = null, v)
    }

    private fun validateUpdate(cr: ChangeRequestEntity, v: ValidationCollector) {
        val payload = payloadMapper.readTermOrNull(cr.afterJson) ?: run { v.error(ValidationCode.EMPTY_PAYLOAD); return }
        val termId = EntityPkParser.parseForValidation(cr.entityPk, EntityPkParser.LABEL_TERM, v) ?: return
        if (!termRepository.existsById(termId)) v.error(ValidationCode.TERM_NOT_FOUND, termId)
        validateFields(payload, v)

        resolveGlossaryIds(payload)?.let { glossaryIds ->
            if (glossaryIds.isEmpty()) { v.error(ValidationCode.EMPTY_GLOSSARY_IDS); return }
            validateGlossaryIds(glossaryIds, v)
        }

        validateMorphemeIds(payload.morphemes, v)
        checkSpellingDuplicate(payload.spelling, excludeId = termId, v)
    }

    private fun validateDelete(cr: ChangeRequestEntity, v: ValidationCollector) {
        val termId = EntityPkParser.parseForValidation(cr.entityPk, EntityPkParser.LABEL_TERM, v) ?: return
        if (!termRepository.existsById(termId)) v.error(ValidationCode.TERM_NOT_FOUND, termId)
    }

    private fun validateFields(payload: TermPayload, v: ValidationCollector) {
        if (payload.spelling.isBlank()) v.error(ValidationCode.SPELLING_REQUIRED)
        if (payload.spelling.length > SPELLING_MAX) v.error(ValidationCode.SPELLING_TOO_LONG, SPELLING_MAX)
        if (payload.meaning.isBlank()) v.error(ValidationCode.MEANING_REQUIRED)
        if (payload.meaning.length > MEANING_MAX) v.error(ValidationCode.MEANING_TOO_LONG, MEANING_MAX)
        payload.abbreviation?.takeIf { it.length > ABBREVIATION_MAX }
            ?.let { v.error(ValidationCode.ABBREVIATION_TOO_LONG, ABBREVIATION_MAX) }
    }

    private fun validateGlossaryIds(glossaryIds: List<Long>, v: ValidationCollector) {
        if (glossaryIds.any { it <= 0 }) { v.error(ValidationCode.GLOSSARY_NOT_FOUND, glossaryIds); return }
        if (glossaryIds.distinct().size != glossaryIds.size) { v.error(ValidationCode.GLOSSARY_ID_DUPLICATE); return }
        val found = glossaryRepository.findAllById(glossaryIds).mapNotNull { it.id }.toSet()
        val missing = glossaryIds.filterNot { it in found }
        if (missing.isNotEmpty()) v.error(ValidationCode.GLOSSARY_NOT_FOUND, missing)
    }

    private fun validateMorphemeIds(ids: List<Long>, v: ValidationCollector) {
        if (ids.isEmpty()) return
        if (ids.any { it <= 0 }) { v.error(ValidationCode.MORPHEME_ID_INVALID); return }
        if (ids.distinct().size != ids.size) { v.error(ValidationCode.MORPHEME_ID_DUPLICATE); return }
        val found = morphemeRepository.findAllById(ids).mapNotNull { it.id }.toSet()
        val missing = ids.filterNot { it in found }
        if (missing.isNotEmpty()) v.error(ValidationCode.MORPHEME_ID_NOT_FOUND, missing)
    }

    private fun checkSpellingDuplicate(spelling: String, excludeId: Long?, v: ValidationCollector) {
        termRepository.findAllBySpellingIgnoreCase(spelling)
            .firstOrNull { it.id != excludeId }
            ?.let { v.warn(ValidationCode.TERM_SPELLING_DUPLICATE, it.spelling, it.id, it.meaning) }
    }

    @Suppress("DEPRECATION")
    private fun resolveGlossaryIds(payload: TermPayload): List<Long>? =
        payload.glossaryIds ?: payload.glossaryId?.let { listOf(it) }
}
