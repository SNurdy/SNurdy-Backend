// Responsibility: 변경 요청의 검증·반영·롤백에 대한 공통 계약 (Strategy 인터페이스)
package com.snurdy.backend.domain.admin.workflow.service.handler

import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.service.model.ValidationResult

interface ChangeRequestHandler {
    fun validate(cr: ChangeRequestEntity): ValidationResult
    fun apply(cr: ChangeRequestEntity): String?
    fun rollback(cr: ChangeRequestEntity)
}
