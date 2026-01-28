package com.snurdy.backend.domain.auth.oauth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.snurdy.backend.domain.auth.oauth.dto.external.KakaoTokenResponse;

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

