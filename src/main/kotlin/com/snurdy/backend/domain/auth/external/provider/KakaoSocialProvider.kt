package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.client.KakaoApiClient
import com.snurdy.backend.domain.auth.external.client.KakaoAuthClient
import com.snurdy.backend.domain.auth.external.dto.KakaoTokenResponse
import com.snurdy.backend.domain.auth.external.dto.KakaoUserInfoResponse
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.user.entity.SocialType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class KakaoSocialProvider(
    private val kakaoAuthClient: KakaoAuthClient,
    private val kakaoApiClient: KakaoApiClient,
    @Value("\${oauth.kakao.client-id}") private val clientId: String,
    @Value("\${oauth.kakao.redirect-uri}") private val redirectUri: String
) : SocialProvider {

    override val socialType: SocialType = SocialType.KAKAO

    override fun getAccessToken(authorizationCode: String): String {
        val params = mapOf(
            "grant_type" to GRANT_TYPE,
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "code" to authorizationCode
        )

        val tokenResponse = kakaoAuthClient.getAccessToken(params)
        return tokenResponse.accessToken
    }

    override fun getSocialUserInfo(accessToken: String): SocialUserInfo {
        val userInfoResponse = kakaoApiClient.getUserInfo("$BEARER_PREFIX$accessToken")
        return userInfoResponse.toSocialUserInfo()
    }

    companion object {
        private const val GRANT_TYPE = "authorization_code"
        private const val BEARER_PREFIX = "Bearer "
    }
}