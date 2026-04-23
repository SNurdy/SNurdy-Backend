package com.snurdy.backend.domain.admin.workflow.service.support

import com.snurdy.backend.domain.admin.workflow.service.model.ValidationCode
import com.snurdy.backend.domain.admin.workflow.service.model.ValidationCollector
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.exception.ApiException

/**
 * ChangeRequest.entityPk 는 String 이지만 TERM/MORPHEME 는 Long PK 다.
 * - 검증 단계에서는 실패를 ValidationCollector 에 누적하고 null 반환
 * - 실행 단계(apply/rollback)에서는 즉시 ApiException 으로 중단
 */
object EntityPkParser {

    const val LABEL_TERM = "용어"
    const val LABEL_MORPHEME = "어근어미"

    /** validate 용: 파싱 실패는 ValidationCollector 에 기록하고 null 반환. */
    fun parseForValidation(entityPk: String, label: String, v: ValidationCollector): Long? {
        val id = entityPk.toLongOrNull()
        if (id == null) {
            v.error(ValidationCode.INVALID_ENTITY_PK, label)
        }
        return id
    }

    /** apply 용: 파싱 실패는 즉시 예외. */
    fun parseForApply(entityPk: String): Long =
        entityPk.toLongOrNull() ?: throw ApiException(AdminError.WORKFLOW_VALIDATION_FAILED)

    /** rollback 용: 파싱 실패는 WORKFLOW_ROLLBACK_FAILED 로 중단. */
    fun parseForRollback(entityPk: String): Long =
        entityPk.toLongOrNull() ?: throw ApiException(AdminError.WORKFLOW_ROLLBACK_FAILED)
}
