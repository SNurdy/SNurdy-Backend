package com.snurdy.backend.domain.auth.client;

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
