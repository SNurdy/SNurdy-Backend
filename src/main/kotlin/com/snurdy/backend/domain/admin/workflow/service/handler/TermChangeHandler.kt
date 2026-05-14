// Responsibility: 용어(Term) 변경 요청의 반영(apply)과 롤백(rollback)
package com.snurdy.backend.domain.admin.workflow.service.handler

import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.TermPayload
import com.snurdy.backend.domain.admin.workflow.service.model.ValidationResult
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestPayloadMapper
import com.snurdy.backend.domain.admin.workflow.service.support.EntityPkParser
import com.snurdy.backend.domain.admin.workflow.service.support.TermRelationManager
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TermChangeHandler(
    private val termRepository: TermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val relationManager: TermRelationManager,
    private val validator: TermValidator,
    private val payloadMapper: ChangeRequestPayloadMapper
) : ChangeRequestHandler {

    override fun validate(cr: ChangeRequestEntity): ValidationResult = validator.validate(cr)

    override fun apply(cr: ChangeRequestEntity): String? = when (cr.operation) {
        ChangeRequestOperation.CREATE -> applyCreate(cr)
        ChangeRequestOperation.UPDATE -> applyUpdate(cr)
        ChangeRequestOperation.DELETE -> applyDelete(cr)
    }

    override fun rollback(cr: ChangeRequestEntity) = when (cr.operation) {
        ChangeRequestOperation.CREATE -> rollbackCreate(cr)
        ChangeRequestOperation.UPDATE -> rollbackUpdate(cr)
        ChangeRequestOperation.DELETE -> rollbackDelete(cr)
    }

    private fun applyCreate(cr: ChangeRequestEntity): String {
        val payload = requireAfterPayload(cr)
        val term = termRepository.save(payload.toTermEntity())
        val termId = term.id ?: throw ApiException(DictionaryError.TERM_NOT_FOUND)
        relationManager.replaceMorphemeMappings(term, payload.morphemes)
        val glossaryIds = resolveGlossaryIds(payload) ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        relationManager.replaceGlossaryMappings(termId, glossaryIds)
        return termId.toString()
    }

    private fun applyUpdate(cr: ChangeRequestEntity): String? {
        val termId = EntityPkParser.parseForApply(cr.entityPk)
        val payload = requireAfterPayload(cr)
        val term = termRepository.findById(termId).orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }
        term.applyPayload(payload)
        termRepository.save(term)
        relationManager.replaceMorphemeMappings(term, payload.morphemes)
        resolveGlossaryIds(payload)?.let { relationManager.replaceGlossaryMappings(termId, it) }
        return null
    }

    private fun applyDelete(cr: ChangeRequestEntity): String? {
        val termId = EntityPkParser.parseForApply(cr.entityPk)
        termMorphemeRepository.deleteByIdTermId(termId)
        glossaryTermRepository.deleteByIdTermId(termId)
        val term = termRepository.findById(termId).orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }
        term.deletedAt = Instant.now()
        termRepository.save(term)
        return null
    }

    private fun rollbackCreate(cr: ChangeRequestEntity) {
        val termId = EntityPkParser.parseForRollback(cr.entityPk)
        termMorphemeRepository.deleteByIdTermId(termId)
        glossaryTermRepository.deleteByIdTermId(termId)
        val term = termRepository.findById(termId).orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
        termRepository.delete(term)
    }

    private fun rollbackUpdate(cr: ChangeRequestEntity) {
        val termId = EntityPkParser.parseForRollback(cr.entityPk)
        val beforePayload = requireBeforePayload(cr)
        val term = termRepository.findById(termId).orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
        term.applyPayload(beforePayload)
        termRepository.save(term)
        relationManager.replaceMorphemeMappings(term, beforePayload.morphemes)
        resolveGlossaryIds(beforePayload)?.let { relationManager.replaceGlossaryMappings(termId, it) }
    }

    private fun rollbackDelete(cr: ChangeRequestEntity) {
        val termId = EntityPkParser.parseForRollback(cr.entityPk)
        val beforePayload = requireBeforePayload(cr)
        val term = termRepository.findById(termId).orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
        term.deletedAt = null
        termRepository.save(term)
        relationManager.replaceMorphemeMappings(term, beforePayload.morphemes)
        resolveGlossaryIds(beforePayload)?.let { relationManager.replaceGlossaryMappings(termId, it) }
    }

    private fun requireAfterPayload(cr: ChangeRequestEntity): TermPayload =
        payloadMapper.readTermOrNull(cr.afterJson) ?: throw ApiException(AdminError.WORKFLOW_VALIDATION_FAILED)

    private fun requireBeforePayload(cr: ChangeRequestEntity): TermPayload =
        payloadMapper.readTermOrNull(cr.beforeJson) ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)

    @Suppress("DEPRECATION")
    private fun resolveGlossaryIds(payload: TermPayload): List<Long>? =
        payload.glossaryIds ?: payload.glossaryId?.let { listOf(it) }
}

private fun TermPayload.toTermEntity() = TermEntity(
    spelling = spelling.trim(),
    meaning = meaning.trim(),
    abbreviation = abbreviation?.trim()?.takeIf { it.isNotBlank() },
    explanation = explanation?.trim()?.takeIf { it.isNotBlank() }
)

private fun TermEntity.applyPayload(payload: TermPayload) {
    spelling = payload.spelling.trim()
    meaning = payload.meaning.trim()
    abbreviation = payload.abbreviation?.trim()?.takeIf { it.isNotBlank() }
    explanation = payload.explanation?.trim()?.takeIf { it.isNotBlank() }
}
