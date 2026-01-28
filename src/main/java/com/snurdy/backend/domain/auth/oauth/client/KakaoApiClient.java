package com.snurdy.backend.domain.auth.oauth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.snurdy.backend.domain.auth.oauth.dto.external.KakaoUserInfoResponse;

@FeignClient(name = "kakaoApiClient", url = "https://kapi.kakao.com")
public interface KakaoApiClient {

	@GetMapping("/v2/user/me")
	KakaoUserInfoResponse getUserInfo(
		@RequestHeader("Authorization") String accessToken
	);
}

