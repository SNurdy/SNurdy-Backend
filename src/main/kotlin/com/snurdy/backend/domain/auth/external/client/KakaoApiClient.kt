package com.snurdy.backend.domain.auth.external.client

import com.snurdy.backend.domain.auth.external.dto.KakaoUserInfoResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(name = "kakaoApiClient", url = "https://kapi.kakao.com")
interface KakaoApiClient {
    @GetMapping("/v2/user/me")
    fun getUserInfo(
        @RequestHeader("Authorization") accessToken: String
    ): KakaoUserInfoResponse
}