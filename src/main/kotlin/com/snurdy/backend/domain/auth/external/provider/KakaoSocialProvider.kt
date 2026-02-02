package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.client.KakaoApiClient
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import org.springframework.stereotype.Component

@Component
class KakaoSocialProvider(
    private val kakaoApiClient: KakaoApiClient
) {
    fun getUserInfo(accessToken: String): SocialUserInfo {
        val userInfoResponse = kakaoApiClient.getUserInfo("Bearer $accessToken")
        return userInfoResponse.toSocialUserInfo()
    }
}