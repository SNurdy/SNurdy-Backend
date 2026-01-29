package com.snurdy.backend.domain.auth.external.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo

data class KakaoTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("refresh_token") val refreshToken: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("refresh_token_expires_in") val refreshTokenExpiresIn: Int,
    val scope: String?
)

data class KakaoUserInfoResponse(
    val id: Long,
    @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount?
) {
    data class KakaoAccount(
        val email: String?,
        val profile: Profile?
    )

    data class Profile(
        val nickname: String?,
        @JsonProperty("profile_image_url") val profileImageUrl: String?,
        @JsonProperty("thumbnail_image_url") val thumbnailImageUrl: String?
    )

    fun toSocialUserInfo(): SocialUserInfo = SocialUserInfo(
        socialId = id.toString(),
        email = kakaoAccount?.email,
        nickname = kakaoAccount?.profile?.nickname ?: "카카오유저",
        profileImageUrl = kakaoAccount?.profile?.profileImageUrl
    )
}