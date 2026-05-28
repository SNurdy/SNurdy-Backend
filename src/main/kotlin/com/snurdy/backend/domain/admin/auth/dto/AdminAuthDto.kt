package com.snurdy.backend.domain.admin.auth.dto

import com.snurdy.backend.domain.admin.auth.entity.AdminUserEntity
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
    val accessTokenExpiresIn: Long,
    val admin: AdminInfo
)

data class AdminInfo(
    val id: UUID,
    val email: String,
    val name: String
)

fun AdminUserEntity.toResponse() = AdminInfo(
    id = id,
    email = email,
    name = name
)
