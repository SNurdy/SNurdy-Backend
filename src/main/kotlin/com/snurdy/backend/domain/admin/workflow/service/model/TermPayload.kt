package com.snurdy.backend.domain.admin.workflow.service.model

data class TermPayload(
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val explanation: String?,
    val morphemes: List<Long>,
    val glossaryId: Long?
)