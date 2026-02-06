package com.snurdy.backend.domain.auth.model

import java.util.UUID

data class RefreshToken(
    val userId: UUID,
    val token: String,
    val ttlSeconds: Long
) {
    companion object {
        fun create(userId: UUID, token: String, ttlDays: Long) = RefreshToken(
            userId = userId,
            token = token,
            ttlSeconds = ttlDays * 24 * 60 * 60
        )
    }
}