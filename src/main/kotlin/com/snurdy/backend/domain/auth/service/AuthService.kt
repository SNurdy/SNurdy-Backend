package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.domain.auth.dto.AppleUser
import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.dto.toUserInfo
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.auth.external.provider.AppleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.GoogleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.KakaoSocialProvider
import com.snurdy.backend.domain.auth.model.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtProperties
import com.snurdy.backend.global.jwt.JwtTokenProvider
import com.snurdy.backend.global.jwt.TokenType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthService(
    private val kakaoSocialProvider: KakaoSocialProvider,
    private val appleSocialProvider: AppleSocialProvider,
    private val googleSocialProvider: GoogleSocialProvider,
    private val userRepository: UserRepository,
    private val userRegistrationService: UserRegistrationService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val accessTokenBlacklistService: AccessTokenBlacklistService,
    private val jwtProperties: JwtProperties
) {

    // 소셜 로그인 3종은 외부 API 호출과 Redis 저장이 흐름의 대부분이라 트랜잭션을 걸지 않음
    // DB 쓰기는 UserRegistrationService 가 자체 트랜잭션으로 처리
    fun kakaoLogin(accessToken: String): TokenResponse =
        processLogin(kakaoSocialProvider.getUserInfo(accessToken), SocialType.KAKAO)

    fun appleLogin(identityToken: String, appleUser: AppleUser?): TokenResponse =
        processLogin(appleSocialProvider.verifyAndGetUserInfo(identityToken, appleUser), SocialType.APPLE)

    fun googleLogin(idToken: String): TokenResponse =
        processLogin(googleSocialProvider.verifyAndGetUserInfo(idToken), SocialType.GOOGLE)

    fun reissue(refreshToken: String): TokenResponse {
        val userId = extractUserIdFromRefreshToken(refreshToken)
        validateRefreshToken(userId, refreshToken)
        return userRepository.findByIdOrNull(userId)
            ?.let(::issueTokens)
            ?: throw ApiException(UserError.NOT_FOUND)
    }

    fun logout(refreshToken: String, accessToken: String? = null) {
        val userId = extractUserIdFromRefreshToken(refreshToken)
        validateRefreshToken(userId, refreshToken)
        refreshTokenRepository.deleteByUserId(userId)
        accessTokenBlacklistService.blacklistAccessToken(accessToken)
    }

    fun issueTokens(user: UserEntity): TokenResponse {
        val tokenPair = jwtTokenProvider.createTokenPair(user.id, user.role.name)
        refreshTokenRepository.save(
            RefreshToken.create(
                userId = user.id,
                token = tokenPair.refreshToken,
                ttlDays = jwtProperties.refreshTokenValidityDays
            )
        )
        return TokenResponse(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            accessTokenExpiresIn = tokenPair.accessTokenExpiresIn,
            refreshTokenExpiresIn = tokenPair.refreshTokenExpiresIn,
            user = user.toUserInfo()
        )
    }

    private fun processLogin(userInfo: SocialUserInfo, socialType: SocialType): TokenResponse {
        val activeUser = userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(socialType, userInfo.socialId)
        if (activeUser != null) {
            return issueTokens(activeUser)
        }

        return createNewUser(userInfo, socialType)
    }

    // 동시 최초 가입 경합 시 늦은 쪽은 unique 충돌을 받고 기존 유저 조회로 복구해 정상 로그인 처리
    private fun createNewUser(userInfo: SocialUserInfo, socialType: SocialType): TokenResponse =
        runCatching {
            userRegistrationService.registerNewUser(userInfo, socialType)
        }.recoverCatching { e ->
            if (e is DataIntegrityViolationException) {
                userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(socialType, userInfo.socialId)
                    ?: throw e
            } else throw e
        }.getOrThrow().let(::issueTokens)

    // access token 을 refresh 자리에 넣는 오용이 저장된 refresh 세션을 지우지 못하게 type 을 강제
    private fun extractUserIdFromRefreshToken(refreshToken: String): UUID {
        if (!jwtTokenProvider.validateToken(refreshToken)) throw ApiException(AuthError.INVALID_TOKEN)
        if (jwtTokenProvider.getTokenType(refreshToken) != TokenType.REFRESH) throw ApiException(AuthError.INVALID_TOKEN)
        return jwtTokenProvider.getUserIdFromToken(refreshToken)
    }

    private fun validateRefreshToken(userId: UUID, refreshToken: String) {
        val savedToken = refreshTokenRepository.findByUserId(userId)
            ?: throw ApiException(AuthError.EXPIRED_TOKEN)
        if (savedToken != refreshToken) {
            refreshTokenRepository.deleteByUserId(userId)
            throw ApiException(AuthError.INVALID_TOKEN)
        }
    }

}
