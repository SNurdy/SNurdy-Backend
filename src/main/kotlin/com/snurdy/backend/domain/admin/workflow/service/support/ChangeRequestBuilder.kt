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
    ): ChangeRequestEntity {
        val (entityPk, beforeJson, afterJson) = when (operation) {
            ChangeRequestOperation.CREATE ->
                Triple(ChangeRequestEntity.NEW_ENTITY_PK, null, payloadMapper.writeRequired(request.toTermPayload()))

            ChangeRequestOperation.UPDATE -> {
                val termId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
                val before = snapshotTerm(termId)
                Triple(termId.toString(), before, payloadMapper.writeRequired(request.toTermPayload()))
            }

            ChangeRequestOperation.DELETE -> {
                val termId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
                val before = snapshotTerm(termId)
                Triple(termId.toString(), before, null)
            }
        }

        return newEntity(request, entityType, operation, entityPk, beforeJson, afterJson)
    }

    private fun buildMorpheme(
        request: CreateChangeRequest,
        entityType: ChangeRequestEntityType,
        operation: ChangeRequestOperation
    ): ChangeRequestEntity {
        val (entityPk, beforeJson, afterJson) = when (operation) {
            ChangeRequestOperation.CREATE ->
                Triple(ChangeRequestEntity.NEW_ENTITY_PK, null, payloadMapper.writeRequired(request.toMorphemePayload()))

            ChangeRequestOperation.UPDATE -> {
                val morphemeId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
                val before = morphemeRepository.findById(morphemeId)
                    .orElseThrow { ApiException(AdminError.MORPHEME_NOT_FOUND) }
                Triple(
                    morphemeId.toString(),
                    payloadMapper.writeRequired(MorphemePayload(spelling = before.spelling, meaning = before.meaning)),
                    payloadMapper.writeRequired(request.toMorphemePayload())
                )
            }

            ChangeRequestOperation.DELETE -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }

        return newEntity(request, entityType, operation, entityPk, beforeJson, afterJson)
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
            glossaryId = glossaryIds.firstOrNull(),
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
        glossaryId = this.glossaryIds?.firstOrNull() ?: this.glossaryId,
        glossaryIds = this.glossaryIds ?: this.glossaryId?.let { listOf(it) }
    )

    private fun CreateChangeRequest.toMorphemePayload() = MorphemePayload(
        spelling = this.spelling?.trim().orEmpty(),
        meaning = this.meaning?.trim().orEmpty()
    )
}
