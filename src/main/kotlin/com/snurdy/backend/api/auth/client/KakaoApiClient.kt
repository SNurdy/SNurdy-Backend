package com.snurdy.backend.api.auth.client

import com.snurdy.backend.api.auth.dto.external.KakaoTokenResponse
import com.snurdy.backend.api.auth.dto.external.KakaoUserInfoResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "kakaoAuthClient", url = "https://kauth.kakao.com")
interface KakaoAuthClient {

    @PostMapping("/oauth/token", consumes = ["application/x-www-form-urlencoded"])
    fun getAccessToken(
        @RequestParam("grant_type") grantType: String = "authorization_code",
        @RequestParam("client_id") clientId: String,
        @RequestParam("redirect_uri") redirectUri: String,
        @RequestParam("code") code: String
    ): KakaoTokenResponse
}

@FeignClient(name = "kakaoApiClient", url = "https://kapi.kakao.com")
interface KakaoApiClient {

    @GetMapping("/v2/user/me")
    fun getUserInfo(
        @RequestHeader("Authorization") accessToken: String
    ): KakaoUserInfoResponse
}