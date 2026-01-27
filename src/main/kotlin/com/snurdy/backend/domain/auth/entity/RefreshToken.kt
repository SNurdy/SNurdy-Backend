package com.snurdy.backend.domain.auth.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID? = null,

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,

    @Column(nullable = false, unique = true, length = 500)
    val token: String,

    @Column(nullable = false)
    val expiryDate: LocalDateTime
)