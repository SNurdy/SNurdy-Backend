package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.user.entity.SocialType

interface SocialProvider {
    val socialType: SocialType
    fun getAccessToken(authorizationCode: String): String
    fun getSocialUserInfo(accessToken: String): SocialUserInfo
}