package com.snurdy.backend.domain.auth.dto

import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.*

@Schema(description = "카카오 로그인 요청")
data class KakaoLoginRequest(
    @field:NotBlank
    @field:Schema(
        description = "카카오 SDK 에서 발급받은 access token",
        example = "kT8xR2vQ5pL9mN3wZ7fH4gB6yC1dA0eJ"
    )
    val accessToken: String
)

@Schema(description = "애플 로그인 요청")
data class AppleLoginRequest(
    @field:NotBlank
    @field:Schema(
        description = "Apple Sign In identityToken (JWT)",
        example = "eyJraWQiOiJZdXl..."
    )
    val identityToken: String,
    @field:Schema(
        description = "최초 로그인 시 Apple 이 한 번 제공하는 사용자 정보. 재로그인 시 null"
    )
    val user: AppleUser? = null
)

@Schema(description = "구글 로그인 요청")
data class GoogleLoginRequest(
    @field:NotBlank
    @field:Schema(
        description = "Google Sign-In 에서 발급받은 id_token (JWT)",
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij..."
    )
    val idToken: String
)

@Schema(description = "애플 유저 상세 정보 (최초 로그인 시에만 전달됨)")
data class AppleUser(
    @field:Schema(description = "이메일", example = "user@privaterelay.appleid.com")
    val email: String?,
    @field:Schema(description = "전체 이름", example = "홍길동")
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
    val nickname: String?,

    @Schema(description = "소셜 로그인 제공자")
    val socialType: SocialType
)

fun UserEntity.toUserInfo() = UserInfo(
    id = id,
    email = email,
    name = name,
    nickname = nickname,
    socialType = socialType
)
