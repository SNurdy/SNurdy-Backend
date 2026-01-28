package com.snurdy.backend.domain.auth.oauth.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
	Long id,

	@JsonProperty("kakao_account")
	KakaoAccount kakaoAccount
) {
	public record KakaoAccount(
		String email,
		Profile profile
	) {}

	public record Profile(
		String nickname,

		@JsonProperty("profile_image_url")
		String profileImageUrl,

		@JsonProperty("thumbnail_image_url")
		String thumbnailImageUrl
	) {}
}