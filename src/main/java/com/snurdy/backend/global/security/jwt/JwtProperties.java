package com.snurdy.backend.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	@NotBlank(message = "JWT 시크릿 키는 필수입니다") String secretKey,
	@Positive(message = "액세스 토큰 만료 시간은 양수여야 합니다") Long accessTokenExpiration,
	@Positive(message = "리프레시 토큰 만료 시간은 양수여야 합니다") Long refreshTokenExpiration
) {}