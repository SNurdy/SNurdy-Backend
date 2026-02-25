package com.snurdy.backend.domain.admin.workflow.service.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.*
import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.catalog.term.entity.TermEntity
import com.snurdy.backend.domain.catalog.term.entity.TermMorphemeEntity
import com.snurdy.backend.domain.catalog.term.entity.TermMorphemeId
import com.snurdy.backend.domain.catalog.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.catalog.term.repository.TermRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.CatalogError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

@Component
class TermChangeHandler(
    private val termRepository: TermRepository,
    private val morphemeRepository: MorphemeRepository,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val SPELLING_MAX = 100
        private const val MEANING_MAX = 255
        private const val ABBREVIATION_MAX = 50
    }

    fun validate(cr: ChangeRequestEntity): ValidationResult {
        val v = ValidationCollector()

        when (cr.operation) {
            ChangeRequestOperation.CREATE -> {
                val payload = parsePayload(cr.afterJson)
                if (payload == null) { v.error(ValidationCode.EMPTY_PAYLOAD); return v.build() }
                validateFields(payload, v)
                validateGlossary(payload.glossaryId, v)
                validateMorphemeIds(payload.morphemes, v)
                checkSpellingDuplicate(payload.spelling, excludeId = null, v)
            }

            ChangeRequestOperation.UPDATE -> {
                val payload = parsePayload(cr.afterJson)
                if (payload == null) { v.error(ValidationCode.EMPTY_PAYLOAD); return v.build() }
                val termId = cr.entityPk.toLongOrNull()
                if (termId == null) { v.error(ValidationCode.INVALID_ENTITY_PK, "용어"); return v.build() }
                if (!termRepository.existsById(termId)) v.error(ValidationCode.TERM_NOT_FOUND, termId)
                validateFields(payload, v)
                validateMorphemeIds(payload.morphemes, v)
                checkSpellingDuplicate(payload.spelling, excludeId = termId, v)
            }

            ChangeRequestOperation.DELETE -> {
                val termId = cr.entityPk.toLongOrNull()
                if (termId == null) { v.error(ValidationCode.INVALID_ENTITY_PK, "용어"); return v.build() }
                if (!termRepository.existsById(termId)) v.error(ValidationCode.TERM_NOT_FOUND, termId)
            }

            else -> v.error(ValidationCode.UNSUPPORTED_OPERATION, cr.operation, "CREATE, UPDATE, DELETE")
        }

        return v.build()
    }

    fun apply(cr: ChangeRequestEntity): String? {
        return when (cr.operation) {
            ChangeRequestOperation.CREATE -> applyCreate(cr)
            ChangeRequestOperation.UPDATE -> applyUpdate(cr)
            ChangeRequestOperation.DELETE -> applyDelete(cr)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    fun rollback(cr: ChangeRequestEntity) {
        when (cr.operation) {
            ChangeRequestOperation.CREATE -> rollbackCreate(cr)
            ChangeRequestOperation.UPDATE -> rollbackUpdate(cr)
            ChangeRequestOperation.DELETE -> rollbackDelete(cr)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun applyCreate(cr: ChangeRequestEntity): String {
        val payload = parsePayload(cr.afterJson)!!
        val glossary = glossaryRepository.findById(payload.glossaryId!!)
            .orElseThrow { ApiException(CatalogError.GLOSSARY_NOT_FOUND) }

        val term = termRepository.save(
            TermEntity(
                spelling = payload.spelling.trim(),
                meaning = payload.meaning.trim(),
                abbreviation = payload.abbreviation?.trim()?.takeIf { it.isNotBlank() },
                explanation = payload.explanation?.trim()?.takeIf { it.isNotBlank() }
            )
        )
        replaceMorphemeMappings(term.id!!, payload.morphemes)
        glossaryTermRepository.save(
            GlossaryTermEntity(
                id = GlossaryTermId(glossaryId = glossary.id!!, termId = term.id),
                glossary = glossary,
                term = term
            )
        )
        return term.id.toString()
    }

    private fun applyUpdate(cr: ChangeRequestEntity): String? {
        val termId = cr.entityPk.toLong()
        val payload = parsePayload(cr.afterJson)!!
        val term = termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }

        term.spelling = payload.spelling.trim()
        term.meaning = payload.meaning.trim()
        term.abbreviation = payload.abbreviation?.trim()?.takeIf { it.isNotBlank() }
        term.explanation = payload.explanation?.trim()?.takeIf { it.isNotBlank() }
        termRepository.save(term)
        replaceMorphemeMappings(termId, payload.morphemes)
        return null
    }

    private fun applyDelete(cr: ChangeRequestEntity): String? {
        val termId = cr.entityPk.toLong()
        termMorphemeRepository.deleteByIdTermId(termId)
        val term = termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
        termRepository.delete(term)
        return null
    }

    private fun rollbackCreate(cr: ChangeRequestEntity) {
        val termId = cr.entityPk.toLongOrNull()
            ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)
        termMorphemeRepository.deleteByIdTermId(termId)
        glossaryTermRepository.deleteByIdTermId(termId)
        val term = termRepository.findById(termId).orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
        termRepository.delete(term)
    }

    private fun rollbackUpdate(cr: ChangeRequestEntity) {
        // UPDATE 롤백 = beforeJson으로 복원
        val termId = cr.entityPk.toLong()
        val beforePayload = parsePayload(cr.beforeJson)
            ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)
        val term = termRepository.findById(termId).orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }

        term.spelling = beforePayload.spelling.trim()
        term.meaning = beforePayload.meaning.trim()
        term.abbreviation = beforePayload.abbreviation?.trim()?.takeIf { it.isNotBlank() }
        term.explanation = beforePayload.explanation?.trim()?.takeIf { it.isNotBlank() }
        termRepository.save(term)
        replaceMorphemeMappings(termId, beforePayload.morphemes)
    }

    private fun rollbackDelete(cr: ChangeRequestEntity) {
        // DELETE 롤백 = beforeJson으로 재생성
        val beforePayload = parsePayload(cr.beforeJson)
            ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)

        val term = termRepository.save(
            TermEntity(
                spelling = beforePayload.spelling.trim(),
                meaning = beforePayload.meaning.trim(),
                abbreviation = beforePayload.abbreviation?.trim()?.takeIf { it.isNotBlank() },
                explanation = beforePayload.explanation?.trim()?.takeIf { it.isNotBlank() }
            )
        )
        replaceMorphemeMappings(term.id!!, beforePayload.morphemes)
    }

    private fun validateFields(payload: TermPayload, v: ValidationCollector) {
        if (payload.spelling.isBlank()) v.error(ValidationCode.SPELLING_REQUIRED)
        if (payload.spelling.length > SPELLING_MAX) v.error(ValidationCode.SPELLING_TOO_LONG, SPELLING_MAX)
        if (payload.meaning.isBlank()) v.error(ValidationCode.MEANING_REQUIRED)
        if (payload.meaning.length > MEANING_MAX) v.error(ValidationCode.MEANING_TOO_LONG, MEANING_MAX)
        if ((payload.abbreviation ?: "").length > ABBREVIATION_MAX) v.error(ValidationCode.ABBREVIATION_TOO_LONG, ABBREVIATION_MAX)
    }

    private fun validateGlossary(glossaryId: Long?, v: ValidationCollector) {
        if (glossaryId == null) { v.error(ValidationCode.GLOSSARY_REQUIRED); return }
        if (!glossaryRepository.existsById(glossaryId)) v.error(ValidationCode.GLOSSARY_NOT_FOUND, glossaryId)
    }

    private fun validateMorphemeIds(ids: List<Long>, v: ValidationCollector) {
        if (ids.isEmpty()) return
        if (ids.any { it <= 0 }) { v.error(ValidationCode.MORPHEME_ID_INVALID); return }
        if (ids.distinct().size != ids.size) { v.error(ValidationCode.MORPHEME_ID_DUPLICATE); return }
        val found = morphemeRepository.findAllById(ids).mapNotNull { it.id }.toSet()
        val missing = ids.filter { it !in found }
        if (missing.isNotEmpty()) v.error(ValidationCode.MORPHEME_ID_NOT_FOUND, missing)
    }

    private fun checkSpellingDuplicate(spelling: String, excludeId: Long?, v: ValidationCollector) {
        val duplicate = termRepository.findBySpellingIgnoreCase(spelling)
        if (duplicate != null && duplicate.id != excludeId) {
            v.warn(ValidationCode.TERM_SPELLING_DUPLICATE, duplicate.spelling, duplicate.id, duplicate.meaning)
        }
    }

    private fun replaceMorphemeMappings(termId: Long, morphemeIds: List<Long>) {
        termMorphemeRepository.deleteByIdTermId(termId)
        if (morphemeIds.isEmpty()) return
        val term = termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
        val morphemes = morphemeRepository.findAllById(morphemeIds).associateBy { it.id!! }
        val mappings = morphemeIds.mapIndexed { index, morphemeId ->
            val morpheme = morphemes[morphemeId] ?: throw ApiException(AdminError.MORPHEME_NOT_FOUND)
            TermMorphemeEntity(id = TermMorphemeId(termId = termId, position = index), term = term, morpheme = morpheme)
        }
        termMorphemeRepository.saveAll(mappings)
    }

    internal fun parsePayload(json: String?): TermPayload? =
        json?.takeIf { it.isNotBlank() }?.let { objectMapper.readValue(it) }
}