package com.snurdy.backend.domain.admin.workflow.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.domain.admin.workflow.dto.*
import com.snurdy.backend.domain.admin.workflow.entity.ChangeSetEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeSetItemEntity
import com.snurdy.backend.domain.admin.workflow.repository.ChangeSetItemRepository
import com.snurdy.backend.domain.admin.workflow.repository.ChangeSetRepository
import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.catalog.morpheme.entity.MorphemeEntity
import com.snurdy.backend.domain.catalog.morpheme.entity.MorphemeVariantEntity
import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.domain.catalog.term.entity.TermEntity
import com.snurdy.backend.domain.catalog.term.entity.TermMorphemeEntity
import com.snurdy.backend.domain.catalog.term.entity.TermMorphemeId
import com.snurdy.backend.domain.catalog.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.catalog.term.repository.TermRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.CatalogError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AdminWorkflowService(
    private val changeSetRepository: ChangeSetRepository,
    private val changeSetItemRepository: ChangeSetItemRepository,
    private val termRepository: TermRepository,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val morphemeRepository: MorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val adminAuditLogRepository: AdminAuditLogRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun createChangeRequest(request: CreateChangeRequest, actorUserId: UUID): ChangeSetDetailResponse {
        val entityType = normalizeEntityType(request.entityType)
        val operation = normalizeOperation(request.operation)
        validateOperationByEntityType(entityType, operation)

        if (request.title.isBlank()) throw ApiException(CommonError.BAD_REQUEST)

        val now = Instant.now()
        val changeSet = changeSetRepository.save(
            ChangeSetEntity(
                title = request.title.trim(),
                description = request.description?.trim()?.takeIf { it.isNotBlank() },
                status = WorkflowStatus.REQUESTED,
                sourceEnv = "DEV",
                targetEnv = "DEV",
                requestedBy = actorUserId,
                createdAt = now,
                updatedAt = now
            )
        )

        val item = createItemForRequest(changeSet.id!!, entityType, operation, request, now)
        changeSetItemRepository.save(item)

        audit(actorUserId, "REQUEST", "change_sets", changeSet.id.toString(), null, changeSet)

        return getChangeSet(changeSet.id)
    }

    @Transactional(readOnly = true)
    fun listChangeSets(status: String?, page: Int, size: Int): ChangeSetPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val p = status?.trim()?.takeIf { it.isNotBlank() }
            ?.let { changeSetRepository.findByStatusOrderByIdDesc(it, pageable) }
            ?: changeSetRepository.findAll(pageable)

        return ChangeSetPageResponse(
            page = p.number,
            size = p.size,
            totalElements = p.totalElements,
            totalPages = p.totalPages,
            items = p.content.map { it.toSummary() }
        )
    }

    @Transactional(readOnly = true)
    fun getChangeSet(changeSetId: Long): ChangeSetDetailResponse {
        val changeSet = findChangeSet(changeSetId)
        val items = changeSetItemRepository.findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId)
        return changeSet.toDetail(items)
    }

    @Transactional
    fun validate(changeSetId: Long, actorUserId: UUID): WorkflowActionResponse {
        val changeSet = findChangeSet(changeSetId)
        if (changeSet.status != WorkflowStatus.REQUESTED) {
            throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)
        }

        val items = changeSetItemRepository.findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId)
        var success = 0
        var failed = 0

        items.forEach { item ->
            val result = runCatching { validateItem(item) }
            if (result.isSuccess) {
                item.status = WorkflowItemStatus.VALIDATED
                item.errorCode = null
                item.errorMessage = null
                success++
            } else {
                item.status = WorkflowItemStatus.FAILED
                item.errorCode = AdminError.WORKFLOW_VALIDATION_FAILED.code
                item.errorMessage = result.exceptionOrNull()?.message ?: AdminError.WORKFLOW_VALIDATION_FAILED.message
                failed++
            }
            item.updatedAt = Instant.now()
        }
        changeSetItemRepository.saveAll(items)

        changeSet.status = if (failed == 0) WorkflowStatus.VALIDATED_WAITING else WorkflowStatus.FAILED
        changeSet.validationSummary = objectMapper.writeValueAsString(mapOf("success" to success, "failed" to failed))
        changeSet.updatedAt = Instant.now()
        changeSetRepository.save(changeSet)

        audit(actorUserId, "VALIDATE", "change_sets", changeSetId.toString(), null, changeSet)

        return WorkflowActionResponse(changeSetId = changeSetId, status = changeSet.status, successCount = success, failedCount = failed)
    }

    @Transactional
    fun apply(changeSetId: Long, actorUserId: UUID): WorkflowActionResponse {

        val changeSet = findChangeSet(changeSetId)
        if (changeSet.status != WorkflowStatus.VALIDATED_WAITING) {
            throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)
        }

        val items = changeSetItemRepository.findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId)
        val now = Instant.now()

        var success = 0
        items.forEach { item ->
            applyItem(item)
            item.status = WorkflowItemStatus.APPLIED
            item.updatedAt = now
            success++
        }
        changeSetItemRepository.saveAll(items)

        changeSet.status = WorkflowStatus.APPLIED_DEV
        changeSet.approvedBy = actorUserId
        changeSet.approvedAt = now
        changeSet.appliedBy = actorUserId
        changeSet.appliedAt = now
        changeSet.applySummary = objectMapper.writeValueAsString(mapOf("success" to success, "failed" to 0))
        changeSet.updatedAt = now
        changeSetRepository.save(changeSet)

        audit(actorUserId, "APPLY", "change_sets", changeSetId.toString(), null, changeSet)

        return WorkflowActionResponse(changeSetId = changeSetId, status = changeSet.status, successCount = success, failedCount = 0)
    }

    @Transactional
    fun cancel(changeSetId: Long, actorUserId: UUID): WorkflowActionResponse {
        val changeSet = findChangeSet(changeSetId)
        if (changeSet.status !in setOf(WorkflowStatus.REQUESTED, WorkflowStatus.FAILED, WorkflowStatus.VALIDATED_WAITING)) {
            throw ApiException(AdminError.WORKFLOW_INVALID_STATUS_TRANSITION)
        }

        val now = Instant.now()
        val items = changeSetItemRepository.findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId)
        var skipped = 0
        items.forEach { item ->
            if (item.status != WorkflowItemStatus.APPLIED) {
                item.status = WorkflowItemStatus.SKIPPED
                item.updatedAt = now
                skipped++
            }
        }
        changeSetItemRepository.saveAll(items)

        changeSet.status = WorkflowStatus.CANCELED
        changeSet.updatedAt = now
        changeSetRepository.save(changeSet)

        audit(actorUserId, "CANCEL", "change_sets", changeSetId.toString(), null, changeSet)

        return WorkflowActionResponse(
            changeSetId = changeSetId,
            status = changeSet.status,
            successCount = 0,
            failedCount = skipped
        )
    }

    private fun validateItem(item: ChangeSetItemEntity) {
        when (item.entityType) {
            WorkflowEntityType.TERM -> validateTermItem(item)
            WorkflowEntityType.MORPHEME -> validateMorphemeItem(item)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        }
    }

    private fun validateTermItem(item: ChangeSetItemEntity) {
        when (item.operation) {
            WorkflowOperation.CREATE -> {
                val payload = parseTermPayload(item.afterJson)
                validateTermPayload(payload, requireGlossary = true)
                if (termRepository.findBySpellingIgnoreCase(payload.spelling) != null) {
                    throw ApiException(AdminError.DUPLICATE_TERM_SPELLING)
                }
                glossaryRepository.findById(payload.glossaryId ?: 0L)
                    .orElseThrow { ApiException(CatalogError.GLOSSARY_NOT_FOUND) }
                validateMorphemeIds(payload.morphemes)
            }

            WorkflowOperation.UPDATE -> {
                val payload = parseTermPayload(item.afterJson)
                validateTermPayload(payload, requireGlossary = false)
                val termId = item.entityPk.toLongOrNull() ?: throw ApiException(CommonError.BAD_REQUEST)
                val existing = termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
                val duplicate = termRepository.findBySpellingIgnoreCase(payload.spelling)
                if (duplicate != null && duplicate.id != existing.id) {
                    throw ApiException(AdminError.DUPLICATE_TERM_SPELLING)
                }
                validateMorphemeIds(payload.morphemes)
            }

            WorkflowOperation.DELETE -> {
                val termId = item.entityPk.toLongOrNull() ?: throw ApiException(CommonError.BAD_REQUEST)
                termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
            }

            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun validateMorphemeItem(item: ChangeSetItemEntity) {
        if (item.operation != WorkflowOperation.CREATE) {
            throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
        val payload = parseMorphemePayload(item.afterJson)
        validateMorphemePayload(payload)
        if (morphemeRepository.findBySpellingIgnoreCase(payload.spelling) != null) {
            throw ApiException(AdminError.DUPLICATE_MORPHEME_SPELLING)
        }
    }

    private fun applyItem(item: ChangeSetItemEntity) {
        when (item.entityType) {
            WorkflowEntityType.TERM -> applyTermItem(item)
            WorkflowEntityType.MORPHEME -> applyMorphemeItem(item)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        }
    }

    private fun applyTermItem(item: ChangeSetItemEntity) {
        when (item.operation) {
            WorkflowOperation.CREATE -> {
                val payload = parseTermPayload(item.afterJson)
                val glossary = glossaryRepository.findById(payload.glossaryId ?: 0L)
                    .orElseThrow { ApiException(CatalogError.GLOSSARY_NOT_FOUND) }
                val created = termRepository.save(
                    TermEntity(
                        spelling = payload.spelling.trim(),
                        meaning = payload.meaning.trim(),
                        abbreviation = payload.abbreviation?.trim()?.takeIf { it.isNotBlank() },
                        explanation = payload.explanation?.trim()?.takeIf { it.isNotBlank() }
                    )
                )
                replaceMappings(created.id!!, payload.morphemes)
                glossaryTermRepository.save(
                    GlossaryTermEntity(
                        id = GlossaryTermId(glossaryId = glossary.id!!, termId = created.id),
                        glossary = glossary,
                        term = created
                    )
                )
                item.entityPk = created.id.toString()
            }

            WorkflowOperation.UPDATE -> {
                val termId = item.entityPk.toLongOrNull() ?: throw ApiException(CommonError.BAD_REQUEST)
                val payload = parseTermPayload(item.afterJson)
                val term = termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
                term.spelling = payload.spelling.trim()
                term.meaning = payload.meaning.trim()
                term.abbreviation = payload.abbreviation?.trim()?.takeIf { it.isNotBlank() }
                term.explanation = payload.explanation?.trim()?.takeIf { it.isNotBlank() }
                termRepository.save(term)
                replaceMappings(termId, payload.morphemes)
            }

            WorkflowOperation.DELETE -> {
                val termId = item.entityPk.toLongOrNull() ?: throw ApiException(CommonError.BAD_REQUEST)
                termMorphemeRepository.deleteByIdTermId(termId)
                val term = termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
                termRepository.delete(term)
            }

            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun applyMorphemeItem(item: ChangeSetItemEntity) {
        val payload = parseMorphemePayload(item.afterJson)
        val created = morphemeRepository.save(
            MorphemeEntity(
                spelling = payload.spelling.trim(),
                meaning = payload.meaning.trim()
            )
        )
        saveGeneratedVariants(created)
        item.entityPk = created.id.toString()
    }

    private fun createItemForRequest(
        changeSetId: Long,
        entityType: String,
        operation: String,
        request: CreateChangeRequest,
        now: Instant
    ): ChangeSetItemEntity {
        return when (entityType) {
            WorkflowEntityType.TERM -> createTermItem(changeSetId, operation, request, now)
            WorkflowEntityType.MORPHEME -> createMorphemeItem(changeSetId, operation, request, now)
            else -> throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        }
    }

    private fun createTermItem(
        changeSetId: Long,
        operation: String,
        request: CreateChangeRequest,
        now: Instant
    ): ChangeSetItemEntity {
        return when (operation) {
            WorkflowOperation.CREATE -> {
                val payload = request.toTermPayload(requireTermId = false)
                ChangeSetItemEntity(
                    changeSetId = changeSetId,
                    entityType = WorkflowEntityType.TERM,
                    entityPk = "NEW",
                    operation = operation,
                    beforeJson = null,
                    afterJson = objectMapper.writeValueAsString(payload),
                    status = WorkflowItemStatus.PENDING,
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                )
            }

            WorkflowOperation.UPDATE -> {
                val termId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
                val before = termRepository.findById(termId)
                    .orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
                    .toPayload(termMorphemeRepository.findOrderedMorphemesByTermId(termId).map { it.morphemeId })
                val payload = request.toTermPayload(requireTermId = true)

                ChangeSetItemEntity(
                    changeSetId = changeSetId,
                    entityType = WorkflowEntityType.TERM,
                    entityPk = termId.toString(),
                    operation = operation,
                    beforeJson = objectMapper.writeValueAsString(before),
                    afterJson = objectMapper.writeValueAsString(payload),
                    status = WorkflowItemStatus.PENDING,
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                )
            }

            WorkflowOperation.DELETE -> {
                val termId = request.termId ?: throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
                val before = termRepository.findById(termId)
                    .orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
                    .toPayload(termMorphemeRepository.findOrderedMorphemesByTermId(termId).map { it.morphemeId })

                ChangeSetItemEntity(
                    changeSetId = changeSetId,
                    entityType = WorkflowEntityType.TERM,
                    entityPk = termId.toString(),
                    operation = operation,
                    beforeJson = objectMapper.writeValueAsString(before),
                    afterJson = null,
                    status = WorkflowItemStatus.PENDING,
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                )
            }

            else -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun createMorphemeItem(
        changeSetId: Long,
        operation: String,
        request: CreateChangeRequest,
        now: Instant
    ): ChangeSetItemEntity {
        if (operation != WorkflowOperation.CREATE) {
            throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }

        val payload = request.toMorphemePayload()
        return ChangeSetItemEntity(
            changeSetId = changeSetId,
            entityType = WorkflowEntityType.MORPHEME,
            entityPk = "NEW",
            operation = operation,
            beforeJson = null,
            afterJson = objectMapper.writeValueAsString(payload),
            status = WorkflowItemStatus.PENDING,
            sortOrder = 0,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun normalizeEntityType(entityType: String): String {
        val normalized = entityType.trim().uppercase()
        if (normalized !in setOf(WorkflowEntityType.TERM, WorkflowEntityType.MORPHEME)) {
            throw ApiException(AdminError.INVALID_WORKFLOW_ENTITY_TYPE)
        }
        return normalized
    }

    private fun normalizeOperation(operation: String): String {
        val normalized = operation.trim().uppercase()
        if (normalized !in setOf(WorkflowOperation.CREATE, WorkflowOperation.UPDATE, WorkflowOperation.DELETE)) {
            throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
        return normalized
    }

    private fun validateOperationByEntityType(entityType: String, operation: String) {
        if (entityType == WorkflowEntityType.MORPHEME && operation != WorkflowOperation.CREATE) {
            throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
        }
    }

    private fun validateTermPayload(payload: TermPayload, requireGlossary: Boolean) {
        if (payload.spelling.isBlank() || payload.meaning.isBlank()) {
            throw ApiException(AdminError.TERM_REQUIRED_FIELDS_MISSING)
        }
        if (payload.spelling.length > 100 || payload.meaning.length > 255 || (payload.abbreviation ?: "").length > 50) {
            throw ApiException(AdminError.INVALID_TERM_FIELD_LENGTH)
        }
        if (requireGlossary && payload.glossaryId == null) {
            throw ApiException(AdminError.GLOSSARY_REQUIRED_FOR_TERM_CREATE)
        }
    }

    private fun validateMorphemePayload(payload: MorphemePayload) {
        if (payload.spelling.isBlank() || payload.meaning.isBlank()) {
            throw ApiException(AdminError.MORPHEME_REQUIRED_FIELDS_MISSING)
        }
        if (payload.spelling.length > 100 || payload.meaning.length > 100) {
            throw ApiException(AdminError.INVALID_MORPHEME_FIELD_LENGTH)
        }
    }

    private fun validateMorphemeIds(ids: List<Long>) {
        if (ids.any { it <= 0 } || ids.distinct().size != ids.size) {
            throw ApiException(AdminError.INVALID_MORPHEME_MAPPING)
        }
        if (ids.isEmpty()) return

        val found = morphemeRepository.findAllById(ids).mapNotNull { it.id }.toSet()
        if (found.size != ids.size) {
            throw ApiException(AdminError.MORPHEME_NOT_FOUND)
        }
    }

    private fun replaceMappings(termId: Long, morphemeIds: List<Long>) {
        termMorphemeRepository.deleteByIdTermId(termId)
        if (morphemeIds.isEmpty()) return

        val term = termRepository.findById(termId).orElseThrow { ApiException(CatalogError.TERM_NOT_FOUND) }
        val morphemes = morphemeRepository.findAllById(morphemeIds).associateBy { it.id!! }

        val mappings = morphemeIds.mapIndexed { index, morphemeId ->
            val morpheme = morphemes[morphemeId] ?: throw ApiException(AdminError.MORPHEME_NOT_FOUND)
            TermMorphemeEntity(
                id = TermMorphemeId(termId = termId, position = index),
                term = term,
                morpheme = morpheme
            )
        }
        termMorphemeRepository.saveAll(mappings)
    }

    private fun saveGeneratedVariants(morpheme: MorphemeEntity) {
        val morphemeId = morpheme.id!!
        morphemeVariantRepository.deleteByMorphemeId(morphemeId)

        val variants = generateVariantsFromSpelling(morpheme.spelling)
        if (variants.isEmpty()) return

        val entities = variants.map {
            MorphemeVariantEntity(
                morpheme = morpheme,
                variant = it.variant,
                base = it.base
            )
        }
        morphemeVariantRepository.saveAll(entities)
    }

    private fun generateVariantsFromSpelling(spelling: String): List<VariantToken> {
        val seen = LinkedHashSet<String>()
        val results = mutableListOf<VariantToken>()

        spelling.split(";", ",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .flatMap { expandOptionalParentheses(it) }
            .forEach { token ->
                val variant = token
                    .replace(Regex("[^a-z0-9]"), "")
                    .trim()
                if (variant.isBlank()) return@forEach
                if (!seen.add(variant)) return@forEach

                val base = if (variant.length > 1 && variant.endsWith("o")) variant.dropLast(1) else variant
                results.add(VariantToken(variant = variant, base = base))
            }

        return results
    }

    private fun expandOptionalParentheses(token: String): List<String> {
        val regex = Regex("^(.*)\\(([^)]+)\\)(.*)$")
        val match = regex.matchEntire(token.trim()) ?: return listOf(token)
        val prefix = match.groupValues[1]
        val optional = match.groupValues[2]
        val suffix = match.groupValues[3]

        val withoutOptional = (prefix + suffix).trim()
        val withOptional = (prefix + optional + suffix).trim()
        return listOf(withoutOptional, withOptional)
    }

    private fun findChangeSet(changeSetId: Long): ChangeSetEntity =
        changeSetRepository.findById(changeSetId).orElseThrow { ApiException(CommonError.NOT_FOUND) }

    private fun parseTermPayload(json: String?): TermPayload {
        if (json.isNullOrBlank()) throw ApiException(CommonError.BAD_REQUEST)
        return objectMapper.readValue(json)
    }

    private fun parseMorphemePayload(json: String?): MorphemePayload {
        if (json.isNullOrBlank()) throw ApiException(CommonError.BAD_REQUEST)
        return objectMapper.readValue(json)
    }

    private fun audit(
        actorUserId: UUID,
        action: String,
        table: String,
        pk: String,
        before: Any?,
        after: Any?
    ) {
        adminAuditLogRepository.save(
            AdminAuditLogEntity(
                actorUserId = actorUserId,
                actorRole = "ADMIN",
                action = action,
                targetTable = table,
                targetPk = pk,
                beforeJson = before?.let { objectMapper.writeValueAsString(it) },
                afterJson = after?.let { objectMapper.writeValueAsString(it) }
            )
        )
    }

    private fun ChangeSetEntity.toSummary(): ChangeSetSummary = ChangeSetSummary(
        id = this.id!!,
        title = this.title,
        status = this.status,
        requestedBy = this.requestedBy.toString(),
        createdAt = this.createdAt.toString(),
        validationSummary = this.validationSummary,
        applySummary = this.applySummary
    )

    private fun ChangeSetEntity.toDetail(items: List<ChangeSetItemEntity>): ChangeSetDetailResponse = ChangeSetDetailResponse(
        id = this.id!!,
        title = this.title,
        description = this.description,
        status = this.status,
        sourceEnv = this.sourceEnv,
        targetEnv = this.targetEnv,
        requestedBy = this.requestedBy.toString(),
        approvedBy = this.approvedBy?.toString(),
        appliedBy = this.appliedBy?.toString(),
        validationSummary = this.validationSummary,
        applySummary = this.applySummary,
        createdAt = this.createdAt.toString(),
        items = items.map {
            ChangeSetItemDetail(
                id = it.id!!,
                operation = it.operation,
                entityType = it.entityType,
                entityPk = it.entityPk,
                status = it.status,
                beforeJson = it.beforeJson,
                afterJson = it.afterJson,
                errorCode = it.errorCode,
                errorMessage = it.errorMessage
            )
        }
    )

    private fun CreateChangeRequest.toTermPayload(requireTermId: Boolean): TermPayload {
        if (requireTermId && this.termId == null) throw ApiException(AdminError.WORKFLOW_TERM_ID_REQUIRED)
        return TermPayload(
            spelling = this.spelling?.trim().orEmpty(),
            meaning = this.meaning?.trim().orEmpty(),
            abbreviation = this.abbreviation?.trim()?.takeIf { it.isNotBlank() },
            explanation = this.explanation?.trim()?.takeIf { it.isNotBlank() },
            morphemes = this.morphemes,
            glossaryId = this.glossaryId
        )
    }

    private fun CreateChangeRequest.toMorphemePayload(): MorphemePayload = MorphemePayload(
        spelling = this.spelling?.trim().orEmpty(),
        meaning = this.meaning?.trim().orEmpty()
    )

    private fun TermEntity.toPayload(morphemes: List<Long>): TermPayload = TermPayload(
        spelling = this.spelling,
        meaning = this.meaning,
        abbreviation = this.abbreviation,
        explanation = this.explanation,
        morphemes = morphemes,
        glossaryId = null
    )
}

private data class TermPayload(
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val explanation: String?,
    val morphemes: List<Long>,
    val glossaryId: Long?
)

private data class MorphemePayload(
    val spelling: String,
    val meaning: String
)

private data class VariantToken(
    val variant: String,
    val base: String
)
