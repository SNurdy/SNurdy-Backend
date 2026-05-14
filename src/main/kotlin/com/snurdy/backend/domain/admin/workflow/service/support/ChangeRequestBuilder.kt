package com.snurdy.backend.domain.admin.workflow.service.support

import com.snurdy.backend.domain.admin.workflow.dto.CreateChangeRequest
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntityType
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.MorphemePayload
import com.snurdy.backend.domain.admin.workflow.service.model.TermPayload
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

@Component
class ChangeRequestBuilder(
    private val termRepository: TermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeRepository: MorphemeRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val payloadMapper: ChangeRequestPayloadMapper
) {

    fun build(
        request: CreateChangeRequest,
        entityType: ChangeRequestEntityType,
        operation: ChangeRequestOperation
    ): ChangeRequestEntity = when (entityType) {
        ChangeRequestEntityType.TERM -> buildTerm(request, entityType, operation)
        ChangeRequestEntityType.MORPHEME -> buildMorpheme(request, entityType, operation)
    }

    private fun buildTerm(
        request: CreateChangeRequest,
        entityType: ChangeRequestEntityType,
        operation: ChangeRequestOperation
    ): ChangeRequestEntity = when (operation) {
        ChangeRequestOperation.CREATE -> buildTermCreate(request, entityType)
        ChangeRequestOperation.UPDATE -> buildTermUpdate(request, entityType)
        ChangeRequestOperation.DELETE -> buildTermDelete(request, entityType)
    }

    private fun buildTermCreate(request: CreateChangeRequest, entityType: ChangeRequestEntityType) =
        newEntity(
            request, entityType, ChangeRequestOperation.CREATE,
            entityPk = ChangeRequestEntity.NEW_ENTITY_PK,
            beforeJson = null,
            afterJson = payloadMapper.writeRequired(request.toTermPayload())
        )

    private fun buildTermUpdate(request: CreateChangeRequest, entityType: ChangeRequestEntityType): ChangeRequestEntity {
        val termId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
        return newEntity(
            request, entityType, ChangeRequestOperation.UPDATE,
            entityPk = termId.toString(),
            beforeJson = snapshotTerm(termId),
            afterJson = payloadMapper.writeRequired(request.toTermPayload())
        )
    }

    private fun buildTermDelete(request: CreateChangeRequest, entityType: ChangeRequestEntityType): ChangeRequestEntity {
        val termId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
        return newEntity(
            request, entityType, ChangeRequestOperation.DELETE,
            entityPk = termId.toString(),
            beforeJson = snapshotTerm(termId),
            afterJson = null
        )
    }

    private fun buildMorpheme(
        request: CreateChangeRequest,
        entityType: ChangeRequestEntityType,
        operation: ChangeRequestOperation
    ): ChangeRequestEntity = when (operation) {
        ChangeRequestOperation.CREATE -> buildMorphemeCreate(request, entityType)
        ChangeRequestOperation.UPDATE -> buildMorphemeUpdate(request, entityType)
        ChangeRequestOperation.DELETE -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
    }

    private fun buildMorphemeCreate(request: CreateChangeRequest, entityType: ChangeRequestEntityType) =
        newEntity(
            request, entityType, ChangeRequestOperation.CREATE,
            entityPk = ChangeRequestEntity.NEW_ENTITY_PK,
            beforeJson = null,
            afterJson = payloadMapper.writeRequired(request.toMorphemePayload())
        )

    private fun buildMorphemeUpdate(request: CreateChangeRequest, entityType: ChangeRequestEntityType): ChangeRequestEntity {
        val morphemeId = request.morphemeId ?: throw ApiException(AdminError.WORKFLOW_MORPHEME_ID_REQUIRED)
        val before = morphemeRepository.findById(morphemeId)
            .orElseThrow { ApiException(AdminError.MORPHEME_NOT_FOUND) }
        return newEntity(
            request, entityType, ChangeRequestOperation.UPDATE,
            entityPk = morphemeId.toString(),
            beforeJson = payloadMapper.writeRequired(MorphemePayload(spelling = before.spelling, meaning = before.meaning)),
            afterJson = payloadMapper.writeRequired(request.toMorphemePayload())
        )
    }

    private fun snapshotTerm(termId: Long): String {
        val term = termRepository.findById(termId).orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }
        val morphemeIds = termMorphemeRepository.findOrderedMorphemesByTermId(termId).map { it.morphemeId }
        val glossaryIds = glossaryTermRepository.findGlossariesByTermId(termId).map { it.glossaryId }
        val payload = TermPayload(
            spelling = term.spelling,
            meaning = term.meaning,
            abbreviation = term.abbreviation,
            explanation = term.explanation,
            morphemes = morphemeIds,
            glossaryIds = glossaryIds
        )
        return payloadMapper.writeRequired(payload)
    }

    private fun newEntity(
        request: CreateChangeRequest,
        entityType: ChangeRequestEntityType,
        operation: ChangeRequestOperation,
        entityPk: String,
        beforeJson: String?,
        afterJson: String?
    ) = ChangeRequestEntity(
        title = request.title.trim(),
        description = request.description?.trim()?.takeIf { it.isNotBlank() },
        entityType = entityType,
        operation = operation,
        entityPk = entityPk,
        beforeJson = beforeJson,
        afterJson = afterJson,
    )

    private fun CreateChangeRequest.toTermPayload() = TermPayload(
        spelling = this.spelling?.trim().orEmpty(),
        meaning = this.meaning?.trim().orEmpty(),
        abbreviation = this.abbreviation?.trim()?.takeIf { it.isNotBlank() },
        explanation = this.explanation?.trim()?.takeIf { it.isNotBlank() },
        morphemes = this.morphemes,
        glossaryIds = this.glossaryIds ?: this.glossaryId?.let { listOf(it) }
    )

    private fun CreateChangeRequest.toMorphemePayload() = MorphemePayload(
        spelling = this.spelling?.trim().orEmpty(),
        meaning = this.meaning?.trim().orEmpty()
    )
}
