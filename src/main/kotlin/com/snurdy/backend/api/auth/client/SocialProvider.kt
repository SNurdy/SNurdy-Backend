package com.snurdy.backend.api.auth.client

import com.snurdy.backend.domain.user.entity.SocialType

interface SocialProvider {
    fun getSocialType(): SocialType
    fun getAccessToken(authorizationCode: String): String
    fun getSocialUserInfo(accessToken: String): SocialUserInfo
}

data class SocialUserInfo(
    val socialId: String,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String? = null
)