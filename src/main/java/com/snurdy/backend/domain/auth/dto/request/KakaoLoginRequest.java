package com.snurdy.backend.domain.auth.dto.request;

public record KakaoLoginRequest(
	String authorizationCode
) {}