package com.snurdy.backend.domain.auth.oauth.provider;

import com.snurdy.backend.domain.auth.oauth.dto.SocialUserInfo;
import com.snurdy.backend.domain.user.entity.SocialType;

public interface SocialProvider {
	SocialType getSocialType();
	String getAccessToken(String authorizationCode);
	SocialUserInfo getSocialUserInfo(String accessToken);
}
