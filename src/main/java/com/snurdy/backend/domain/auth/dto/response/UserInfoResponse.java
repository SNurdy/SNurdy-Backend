package com.snurdy.backend.domain.auth.dto.response;

import com.snurdy.backend.domain.user.entity.SocialType;

public record UserInfoResponse(
	Long userId,
	String email,
	String nickname,
	String profileImageUrl,
	SocialType socialType
) {}
