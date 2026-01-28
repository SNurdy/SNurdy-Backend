package com.snurdy.backend.domain.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.snurdy.backend.domain.auth.dto.external.KakaoTokenResponse;
import com.snurdy.backend.domain.auth.dto.external.KakaoUserInfoResponse;

@FeignClient(name = "kakaoAuthClient", url = "https://kauth.kakao.com")
public interface KakaoAuthClient {

	@PostMapping(value = "/oauth/token", consumes = "application/x-www-form-urlencoded")
	KakaoTokenResponse getAccessToken(
		@RequestParam("grant_type") String grantType,
		@RequestParam("client_id") String clientId,
		@RequestParam("redirect_uri") String redirectUri,
		@RequestParam("code") String code
	);
}

@FeignClient(name = "kakaoApiClient", url = "https://kapi.kakao.com")
interface KakaoApiClient {

	@GetMapping("/v2/user/me")
	KakaoUserInfoResponse getUserInfo(
		@RequestHeader("Authorization") String accessToken
	);
}
