package com.snurdy.backend.domain.admin.changeset.model

import java.util.UUID

data class AdminActor(
    val userId: UUID,
    val roles: Set<String>
)

data class RequestMetadata(
    val requestId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null
)
