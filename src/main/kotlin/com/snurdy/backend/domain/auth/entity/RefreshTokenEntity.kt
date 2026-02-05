package com.snurdy.backend.domain.auth.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID? = null,

    @Column(nullable = false, unique = true, columnDefinition = "BINARY(16)")
    val userId: UUID,

    @Column(nullable = false, unique = true, length = 500)
    var token: String,

    @Column(nullable = false)
    var expiryDate: LocalDateTime
) {
    val isExpired: Boolean
        get() = LocalDateTime.now().isAfter(expiryDate)

    fun updateToken(newToken: String, newExpiryDate: LocalDateTime) = apply {
        token = newToken
        expiryDate = newExpiryDate
    }

    companion object {
        fun create(userId: UUID, token: String, validityDays: Long) = RefreshTokenEntity(
            userId = userId,
            token = token,
            expiryDate = LocalDateTime.now().plusDays(validityDays)
        )
    }
}