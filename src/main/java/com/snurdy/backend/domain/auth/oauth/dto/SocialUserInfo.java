package com.snurdy.backend.domain.auth.oauth.dto;

public record SocialUserInfo(
	String socialId,
	String email,
	String nickname,
	String profileImageUrl
) {
	public SocialUserInfo(String socialId, String email, String nickname) {
		this(socialId, email, nickname, null);
	}
}
