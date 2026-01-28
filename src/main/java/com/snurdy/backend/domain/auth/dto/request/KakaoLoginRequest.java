package com.snurdy.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
	@NotBlank String authorizationCode
) {}