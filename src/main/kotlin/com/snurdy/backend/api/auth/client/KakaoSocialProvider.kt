package com.snurdy.backend.api.auth.client

import com.snurdy.backend.domain.user.entity.SocialType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KakaoSocialProvider(
    private val kakaoAuthClient: KakaoAuthClient,
    private val kakaoApiClient: KakaoApiClient,
    @Value("\${oauth.kakao.client-id}") private val clientId: String,
    @Value("\${oauth.kakao.redirect-uri}") private val redirectUri: String
) : SocialProvider {

    override fun getSocialType() = SocialType.KAKAO

    override fun getAccessToken(authorizationCode: String): String {
        val response = kakaoAuthClient.getAccessToken(
            clientId = clientId,
            redirectUri = redirectUri,
            code = authorizationCode
        )
        return response.accessToken
    }

    override fun getSocialUserInfo(accessToken: String): SocialUserInfo {
        val response = kakaoApiClient.getUserInfo("Bearer $accessToken")

        return SocialUserInfo(
            socialId = response.id.toString(),
            email = response.kakaoAccount?.email,
            nickname = response.kakaoAccount?.profile?.nickname,
            profileImageUrl = response.kakaoAccount?.profile?.profileImageUrl
        )
    }
}