package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.client.KakaoApiClient
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.auth.external.dto.toResponse
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.exception.ApiException
import feign.FeignException
import org.springframework.stereotype.Component

@Component
class KakaoSocialProvider(
    private val kakaoApiClient: KakaoApiClient
) {
    fun getUserInfo(accessToken: String): SocialUserInfo {
        val userInfoResponse = try {
            kakaoApiClient.getUserInfo("Bearer $accessToken")
        } catch (e: FeignException.Unauthorized) {
            // 카카오 401 은 클라이언트가 보낸 access token 이 무효라는 의미라 외부 장애가 아님
            throw ApiException(AuthError.INVALID_TOKEN, cause = e)
        }

        return userInfoResponse.toResponse()
    }
}