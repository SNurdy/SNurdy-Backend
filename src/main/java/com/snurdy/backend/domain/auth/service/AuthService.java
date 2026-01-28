package com.snurdy.backend.domain.auth.service;

import com.snurdy.backend.domain.auth.oauth.provider.SocialProvider;
import com.snurdy.backend.domain.auth.oauth.dto.SocialUserInfo;
import com.snurdy.backend.domain.auth.dto.response.TokenResponse;
import com.snurdy.backend.domain.auth.entity.RefreshToken;
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository;
import com.snurdy.backend.domain.user.entity.SocialType;
import com.snurdy.backend.domain.user.entity.User;
import com.snurdy.backend.domain.user.repository.UserRepository;
import com.snurdy.backend.global.common.constants.ErrorType;
import com.snurdy.backend.global.exception.CustomException;
import com.snurdy.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final List<SocialProvider> socialProviders;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final TransactionTemplate transactionTemplate;

	public TokenResponse socialLogin(SocialType socialType, String authorizationCode) {
		// 외부 API 통신 -> 트랜잭션 외부
		SocialUserInfo userInfo = fetchSocialUserInfo(socialType, authorizationCode);

		// DB 작업 및 토큰 발행 -> 트랜잭션 내부
		return Objects.requireNonNull(transactionTemplate.execute(status -> {
			User user = loginOrRegister(socialType, userInfo);
			return issueTokens(user);
		}));
	}

	private SocialUserInfo fetchSocialUserInfo(SocialType socialType, String code) {
		SocialProvider provider = socialProviders.stream()
			.filter(p -> p.getSocialType() == socialType)
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorType.INVALID_SOCIAL_TYPE));

		String accessToken = provider.getAccessToken(code);
		return provider.getSocialUserInfo(accessToken);
	}

	private User loginOrRegister(SocialType type, SocialUserInfo info) {
		return userRepository.findBySocialTypeAndSocialId(type, info.socialId())
			.orElseGet(() -> userRepository.save(User.from(info, type)));
	}

	private TokenResponse issueTokens(User user) {
		TokenResponse tokenResponse = jwtTokenProvider.createToken(user.getId());

		refreshTokenRepository.deleteByUserId(user.getId());
		refreshTokenRepository.save(RefreshToken.of(
			user.getId(),
			tokenResponse.refreshToken(),
			30
		));

		return tokenResponse;
	}
}