package com.snurdy.backend.domain.auth.dto

import com.snurdy.backend.domain.user.entity.SocialType
import jakarta.validation.constraints.NotBlank

object AuthDto {

    data class LoginRequest(
        @field:NotBlank
        val authorizationCode: String
    )

    data class TokenResponse(
        val accessToken: String,
        val refreshToken: String,
        val accessTokenExpiresIn: Long,
        val refreshTokenExpiresIn: Long
    )

    data class UserInfoResponse(
        val userId: Long,
        val email: String,
        val nickname: String,
        val profileImageUrl: String?,
        val socialType: SocialType
    )
}