// Responsibility: 형태소(Morpheme) 변경 요청의 반영(apply)과 롤백(rollback)
package com.snurdy.backend.domain.admin.workflow.service.handler

import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestOperation
import com.snurdy.backend.domain.admin.workflow.service.model.MorphemePayload
import com.snurdy.backend.domain.admin.workflow.service.model.ValidationResult
import com.snurdy.backend.domain.admin.workflow.service.support.ChangeRequestPayloadMapper
import com.snurdy.backend.domain.admin.workflow.service.support.EntityPkParser
import com.snurdy.backend.domain.admin.workflow.service.support.MorphemeVariantGenerator
import com.snurdy.backend.domain.dictionary.morpheme.entity.MorphemeEntity
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
    private val validator: MorphemeValidator,
    private val payloadMapper: ChangeRequestPayloadMapper
) : ChangeRequestHandler {

    override fun validate(cr: ChangeRequestEntity): ValidationResult = validator.validate(cr)

    override fun apply(cr: ChangeRequestEntity): String? = when (cr.operation) {
        ChangeRequestOperation.CREATE -> applyCreate(cr)
        ChangeRequestOperation.UPDATE -> applyUpdate(cr)
        ChangeRequestOperation.DELETE -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
    }

    override fun rollback(cr: ChangeRequestEntity) = when (cr.operation) {
        ChangeRequestOperation.CREATE -> rollbackCreate(cr)
        ChangeRequestOperation.UPDATE -> rollbackUpdate(cr)
        ChangeRequestOperation.DELETE -> throw ApiException(AdminError.INVALID_WORKFLOW_OPERATION)
    }

    private fun applyCreate(cr: ChangeRequestEntity): String {
        val payload = requireAfterPayload(cr)
        val morpheme = morphemeRepository.save(
            MorphemeEntity(spelling = payload.spelling.trim(), meaning = payload.meaning.trim())
        )
        variantGenerator.regenerate(morpheme)
        return morpheme.id?.toString() ?: throw ApiException(AdminError.MORPHEME_NOT_FOUND)
    }

    private fun applyUpdate(cr: ChangeRequestEntity): String? {
        val morphemeId = EntityPkParser.parseForApply(cr.entityPk)
        val payload = requireAfterPayload(cr)
        val morpheme = morphemeRepository.findById(morphemeId)
            .orElseThrow { ApiException(AdminError.MORPHEME_NOT_FOUND) }
        morpheme.spelling = payload.spelling.trim()
        morpheme.meaning = payload.meaning.trim()
        morphemeRepository.save(morpheme)
        variantGenerator.regenerate(morpheme)
        return null
    }

    private fun rollbackCreate(cr: ChangeRequestEntity) {
        val morphemeId = EntityPkParser.parseForRollback(cr.entityPk)
        morphemeVariantRepository.deleteByMorphemeId(morphemeId)
        val morpheme = morphemeRepository.findById(morphemeId)
            .orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
        morphemeRepository.delete(morpheme)
    }

    private fun rollbackUpdate(cr: ChangeRequestEntity) {
        val morphemeId = EntityPkParser.parseForRollback(cr.entityPk)
        val beforePayload = requireBeforePayload(cr)
        val morpheme = morphemeRepository.findById(morphemeId)
            .orElseThrow { ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED) }
        morpheme.spelling = beforePayload.spelling.trim()
        morpheme.meaning = beforePayload.meaning.trim()
        morphemeRepository.save(morpheme)
        variantGenerator.regenerate(morpheme)
    }

    private fun requireAfterPayload(cr: ChangeRequestEntity): MorphemePayload =
        payloadMapper.readMorphemeOrNull(cr.afterJson) ?: throw ApiException(AdminError.WORKFLOW_VALIDATION_FAILED)

    private fun requireBeforePayload(cr: ChangeRequestEntity): MorphemePayload =
        payloadMapper.readMorphemeOrNull(cr.beforeJson) ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)
}
