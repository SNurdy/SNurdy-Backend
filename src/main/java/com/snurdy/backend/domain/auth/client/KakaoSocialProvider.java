package com.snurdy.backend.domain.auth.client;

import com.snurdy.backend.domain.auth.dto.external.KakaoUserInfoResponse;
import com.snurdy.backend.domain.user.entity.SocialType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KakaoSocialProvider implements SocialProvider {

	private final KakaoAuthClient kakaoAuthClient;
	private final KakaoApiClient kakaoApiClient;
	private final String clientId;
	private final String redirectUri;

	public KakaoSocialProvider(
		KakaoAuthClient kakaoAuthClient,
		KakaoApiClient kakaoApiClient,
		@Value("${oauth.kakao.client-id}") String clientId,
		@Value("${oauth.kakao.redirect-uri}") String redirectUri) {
		this.kakaoAuthClient = kakaoAuthClient;
		this.kakaoApiClient = kakaoApiClient;
		this.clientId = clientId;
		this.redirectUri = redirectUri;
	}

	@Override
	public SocialType getSocialType() {
		return SocialType.KAKAO;
	}

	@Override
	public String getAccessToken(String authorizationCode) {
		var response = kakaoAuthClient.getAccessToken(
			"authorization_code",
			clientId,
			redirectUri,
			authorizationCode
		);
		return response.accessToken();
	}

	@Override
	public SocialUserInfo getSocialUserInfo(String accessToken) {
		var response = kakaoApiClient.getUserInfo("Bearer " + accessToken);

		KakaoUserInfoResponse.KakaoAccount account = response.kakaoAccount();
		KakaoUserInfoResponse.Profile profile = account != null ? account.profile() : null;

		return new SocialUserInfo(
			String.valueOf(response.id()),
			account != null ? account.email() : null,
			profile != null ? profile.nickname() : null,
			profile != null ? profile.profileImageUrl() : null
		);
	}
}