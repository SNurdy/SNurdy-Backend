package com.snurdy.backend.domain.admin.workflow.service.model

data class TermPayload(
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val explanation: String?,
    val morphemes: List<Long>,
    val glossaryIds: List<Long>?,
    @Deprecated("Legacy field retained for deserialization of existing DB records. Use glossaryIds.")
    val glossaryId: Long? = null
)