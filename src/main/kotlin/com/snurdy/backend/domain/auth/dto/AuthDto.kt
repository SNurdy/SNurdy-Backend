package com.snurdy.backend.domain.auth.dto

import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.*

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

@Schema(description = "구글 로그인 요청")
data class GoogleLoginRequest(
    @field:NotBlank
    @Schema(description = "Google id_token", required = true)
    val idToken: String
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
    val refreshTokenExpiresIn: Long,
    val user: UserInfo
)

@Schema(description = "유저 정보")
data class UserInfo(
    @Schema(description = "유저 ID")
    val id: UUID,

    @Schema(description = "이메일")
    val email: String?,

    @Schema(description = "이름")
    val name: String?,

    @Schema(description = "닉네임")
    val nickname: String,

    @Schema(description = "소셜 로그인 제공자")
    val provider: SocialType
)

fun UserEntity.toUserInfo() = UserInfo(
    id = id,
    email = email,
    name = name,
    nickname = nickname,
    provider = socialType
)