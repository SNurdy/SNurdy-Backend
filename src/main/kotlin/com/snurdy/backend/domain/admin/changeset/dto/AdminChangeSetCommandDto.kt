package com.snurdy.backend.domain.admin.changeset.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateChangeSetRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val title: String,

    @field:Size(max = 1000)
    val description: String? = null,

    @field:NotBlank
    val sourceEnv: String = "DEV",

    @field:NotBlank
    val targetEnv: String = "PROD"
)

data class CreateChangeSetResponse(
    val changeSetId: Long,
    val status: String
)

data class UpsertTermChangeSetItemRequest(
    @field:NotBlank
    val operation: String,

    val termId: Long? = null,

    @field:Size(max = 100)
    val spelling: String? = null,

    @field:Size(max = 255)
    val meaning: String? = null,

    @field:Size(max = 50)
    val abbreviation: String? = null,

    val explanation: String? = null,

    val sortOrder: Int? = null
)

data class ChangeSetItemCommandResponse(
    val changeSetId: Long,
    val itemId: Long,
    val operation: String,
    val status: String
)
