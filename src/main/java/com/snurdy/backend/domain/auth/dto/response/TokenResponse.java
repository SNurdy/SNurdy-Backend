package com.snurdy.backend.domain.auth.dto.response;

public record TokenResponse(
	String accessToken,
	String refreshToken,
	Long accessTokenExpiresIn,
	Long refreshTokenExpiresIn
) {}
