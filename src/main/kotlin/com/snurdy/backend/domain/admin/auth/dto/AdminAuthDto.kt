package com.snurdy.backend.domain.admin.auth.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AdminLoginRequest(
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String
)

data class AdminTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long,
    val refreshTokenExpiresIn: Long,
    val admin: AdminInfo
)

data class AdminInfo(
    val id: UUID,
    val email: String,
    val name: String
)