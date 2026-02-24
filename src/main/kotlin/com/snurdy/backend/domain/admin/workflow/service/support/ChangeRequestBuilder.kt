package com.snurdy.backend.domain.admin.workflow.service.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.snurdy.backend.domain.admin.workflow.dto.CreateChangeRequest
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntityType
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.MorphemePayload
import com.snurdy.backend.domain.admin.workflow.service.model.TermPayload
import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.catalog.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.catalog.term.repository.TermRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.CatalogError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ChangeRequestBuilder(
    private val termRepository: TermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeRepository: MorphemeRepository,
    private val objectMapper: ObjectMapper
) {

    fun build(request: CreateChangeRequest, entityType: String, operation: String, actorUserId: UUID): ChangeRequestEntity {
        return when (entityType) {
            ChangeRequestEntityType.TERM -> buildTerm(request, entityType, operation, actorUserId)
            ChangeRequestEntityType.MORPHEME -> buildMorpheme(request, entityType, operation, actorUserId)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        }
    }

    private fun buildTerm(
        request: CreateChangeRequest, entityType: String, operation: String, actorUserId: UUID
    ): ChangeRequestEntity {
        val (entityPk, beforeJson, afterJson) = when (operation) {
            ChangeRequestOperation.CREATE ->
                Triple("NEW", null, objectMapper.writeValueAsString(request.toTermPayload()))

            ChangeRequestOperation.UPDATE -> {
                val termId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
                val before = snapshotTerm(termId)
                Triple(termId.toString(), before, objectMapper.writeValueAsString(request.toTermPayload()))
            }

            ChangeRequestOperation.DELETE -> {
                val termId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
                val before = snapshotTerm(termId)
                Triple(termId.toString(), before, null)
            }

            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }

        return newEntity(request, entityType, operation, entityPk, beforeJson, afterJson, actorUserId)
    }

    private fun buildMorpheme(
        request: CreateChangeRequest, entityType: String, operation: String, actorUserId: UUID
    ): ChangeRequestEntity {
        val (entityPk, beforeJson, afterJson) = when (operation) {
            ChangeRequestOperation.CREATE ->
                Triple("NEW", null, objectMapper.writeValueAsString(request.toMorphemePayload()))

            ChangeRequestOperation.UPDATE -> {
                val morphemeId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
                val before = morphemeRepository.findById(morphemeId)
                    .orElseThrow { ApiException(AdminError.MORPHEME_NOT_FOUND) }
                Triple(
                    morphemeId.toString(),
                    objectMapper.writeValueAsString(MorphemePayload(spelling = before.spelling, meaning = before.meaning)),
                    objectMapper.writeValueAsString(request.toMorphemePayload())
                )
            }

            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }

        return newEntity(request, entityType, operation, entityPk, beforeJson, afterJson, actorUserId)
    }

    private fun snapshotTerm(termId: Long): String {
        val term = termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
        val morphemeIds = termMorphemeRepository.findOrderedMorphemesByTermId(termId).map { it.morphemeId }
        val payload = TermPayload(
            spelling = term.spelling,
            meaning = term.meaning,
            abbreviation = term.abbreviation,
            explanation = term.explanation,
            morphemes = morphemeIds,
            glossaryId = null
        )
        return objectMapper.writeValueAsString(payload)
    }

    private fun newEntity(
        request: CreateChangeRequest, entityType: String, operation: String,
        entityPk: String, beforeJson: String?, afterJson: String?, actorUserId: UUID
    ) = ChangeRequestEntity(
        title = request.title.trim(),
        description = request.description?.trim()?.takeIf { it.isNotBlank() },
        entityType = entityType,
        operation = operation,
        entityPk = entityPk,
        beforeJson = beforeJson,
        afterJson = afterJson,
        requestedBy = actorUserId
    )

    private fun CreateChangeRequest.toTermPayload() = TermPayload(
        spelling = this.spelling?.trim().orEmpty(),
        meaning = this.meaning?.trim().orEmpty(),
        abbreviation = this.abbreviation?.trim()?.takeIf { it.isNotBlank() },
        explanation = this.explanation?.trim()?.takeIf { it.isNotBlank() },
        morphemes = this.morphemes,
        glossaryId = this.glossaryId
    )

    private fun CreateChangeRequest.toMorphemePayload() = MorphemePayload(
        spelling = this.spelling?.trim().orEmpty(),
        meaning = this.meaning?.trim().orEmpty()
    )
}