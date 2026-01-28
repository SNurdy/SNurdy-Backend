package com.snurdy.backend.domain.auth.oauth.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import com.snurdy.backend.domain.auth.oauth.dto.external.KakaoTokenResponse;

@FeignClient(name = "kakaoAuthClient", url = "https://kauth.kakao.com")
public interface KakaoAuthClient {

	@PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	KakaoTokenResponse getAccessToken(@SpringQueryMap Map<String, ?> params);
}

