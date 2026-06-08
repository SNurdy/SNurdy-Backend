package com.snurdy.backend.domain.user.profile.dto

import com.snurdy.backend.domain.user.account.entity.UserEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "v2 사용자 프로필 응답")
data class UserProfileResponse(
    val id: UUID,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val marketingConsent: Boolean,
    val marketingConsentAt: Instant?,
    val createdAt: Instant?
)

@Schema(description = "v2 사용자 프로필 부분 수정 요청 (PATCH)")
data class UserProfileUpdateRequest(
    @field:Size(max = 20)
    @field:Schema(description = "변경할 닉네임", example = "닉네임")
    val nickname: String? = null,

    @field:Size(max = 512)
    @field:Schema(description = "변경할 프로필 이미지 URL", example = "https://cdn.example.com/profiles/abc.png")
    val profileImageUrl: String? = null
)

@Schema(description = "마케팅 수신 동의 변경 요청")
data class MarketingConsentRequest(
    @field:Schema(description = "동의 여부", example = "true")
    val agreed: Boolean
)

@Schema(description = "마케팅 수신 동의 응답")
data class MarketingConsentResponse(
    val marketingConsent: Boolean,
    val marketingConsentAt: Instant?
)

// UserEntity 는 auth 의 toUserInfo() 와 동시 사용되어 1:N 케이스 - 의미 키워드로 구분
fun UserEntity.toProfileResponse() = UserProfileResponse(
    id = id,
    email = email,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    marketingConsent = marketingConsent,
    marketingConsentAt = marketingConsentAt,
    createdAt = createdAt
)

fun UserEntity.toMarketingConsentResponse() = MarketingConsentResponse(
    marketingConsent = marketingConsent,
    marketingConsentAt = marketingConsentAt
)
