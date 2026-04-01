package com.snurdy.backend.domain.auth.external.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo

data class KakaoUserInfoResponse(
    val id: Long,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount?
) {
    data class KakaoAccount(
        val email: String?,
        val name: String?,
        val profile: Profile?
    )

    data class Profile(
        val nickname: String?
    )

    fun toSocialUserInfo() = SocialUserInfo(
        socialId = id.toString(),
        email = kakaoAccount?.email,
        name = kakaoAccount?.name,
        nickname = kakaoAccount?.profile?.nickname
    )
}