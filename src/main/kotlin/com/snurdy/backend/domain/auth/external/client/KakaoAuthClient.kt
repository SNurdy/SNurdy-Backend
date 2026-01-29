package com.snurdy.backend.domain.auth.external.client

import com.snurdy.backend.domain.auth.external.dto.KakaoTokenResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.cloud.openfeign.SpringQueryMap
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping

@FeignClient(name = "kakaoAuthClient", url = "https://kauth.kakao.com")
interface KakaoAuthClient {
    @PostMapping(
        value = ["/oauth/token"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun getAccessToken(
        @SpringQueryMap params: Map<String, Any>
    ): KakaoTokenResponse
}