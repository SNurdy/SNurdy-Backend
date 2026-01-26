package com.snurdy.backend.domain.auth.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.*

@Entity
class RefreshToken(
    @Id
    val userId: UUID,      // 유저당 하나의 토큰만 유지하도록 PK로 설정
    val token: String,
    val expiryDate: LocalDateTime
)