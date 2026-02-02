package com.snurdy.backend.domain.auth.dto

import com.snurdy.backend.domain.user.entity.SocialType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "인증 관련 DTO")
object AuthDto {

    @Schema(description = "카카오 로그인 요청")
    data class KakaoLoginRequest(
        @field:NotBlank
        val accessToken: String
    )

    @Schema(description = "애플 로그인 요청")
    data class AppleLoginRequest(
        @field:NotBlank
        val identityToken: String,
        val user: AppleUser? = null
    )

    @Schema(description = "애플 유저 상세 정보")
    data class AppleUser(
        val email: String?,
        val fullName: String?
    )

    @Schema(description = "서비스 토큰 응답")
    data class TokenResponse(
        val accessToken: String,
        val refreshToken: String,
        val accessTokenExpiresIn: Long,
        val refreshTokenExpiresIn: Long
    )
}