package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.client.KakaoApiClient
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KakaoSocialProvider(
    private val kakaoApiClient: KakaoApiClient
) {
    fun getUserInfo(accessToken: String): SocialUserInfo {
        val userInfoResponse = kakaoApiClient.getUserInfo("Bearer $accessToken")

        log.info("카카오 응답: id=${userInfoResponse.id}, email=${userInfoResponse.kakaoAccount?.email}, name=${userInfoResponse.kakaoAccount?.name}, nickname=${userInfoResponse.kakaoAccount?.profile?.nickname}")

        return userInfoResponse.toSocialUserInfo()
    }

    companion object {
        private val log = LoggerFactory.getLogger(KakaoSocialProvider::class.java)
    }
}