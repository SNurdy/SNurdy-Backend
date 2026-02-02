package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.domain.auth.dto.AuthDto
import com.snurdy.backend.domain.auth.entity.RefreshTokenEntity
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.auth.external.provider.AppleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.KakaoSocialProvider
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.common.constants.ErrorCode
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.security.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val kakaoSocialProvider: KakaoSocialProvider,
    private val appleSocialProvider: AppleSocialProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun kakaoLogin(accessToken: String): AuthDto.TokenResponse {
        val userInfo = kakaoSocialProvider.getUserInfo(accessToken)
        val user = findOrCreateUser(userInfo, SocialType.KAKAO)
        return issueTokens(user.id)
    }

    @Transactional
    fun appleLogin(identityToken: String, appleUser: AuthDto.AppleUser?): AuthDto.TokenResponse {
        val userInfo = appleSocialProvider.verifyAndGetUserInfo(identityToken, appleUser)
        val user = findOrCreateUser(userInfo, SocialType.APPLE)
        return issueTokens(user.id)
    }

    @Transactional
    fun reissue(refreshToken: String): AuthDto.TokenResponse {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw ApiException(ErrorCode.INVALID_TOKEN)
        }

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        validateRefreshToken(userId, refreshToken)

        return issueTokens(userId)
    }

    @Transactional
    fun logout(refreshToken: String) {
        runCatching {
            if (!jwtTokenProvider.validateToken(refreshToken)) return@runCatching
            val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
            refreshTokenRepository.deleteByUserId(userId)
        }.onFailure { e ->
            log.error("로그아웃 처리 중 에러 발생 - 사용자 ID 추적용 토큰: ${refreshToken.take(10)}...", e)
        }
    }

    private fun findOrCreateUser(userInfo: SocialUserInfo, socialType: SocialType): UserEntity {
        return try {
            userRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId)
                ?: userRepository.save(UserEntity.from(userInfo, socialType))
        } catch (e: DataIntegrityViolationException) {
            userRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId) ?: throw e
        }
    }

    private fun validateRefreshToken(userId: UUID, refreshToken: String) {
        val savedEntity = refreshTokenRepository.findByUserId(userId)
            ?: throw ApiException(ErrorCode.EXPIRED_TOKEN)

        if (savedEntity.token != refreshToken) {
            refreshTokenRepository.deleteByUserId(userId)
            throw ApiException(ErrorCode.INVALID_TOKEN)
        }
    }

    private fun issueTokens(userId: UUID): AuthDto.TokenResponse {
        val tokenResponse = jwtTokenProvider.createToken(userId)

        val refreshTokenEntity = refreshTokenRepository.findByUserId(userId)
            ?.updateToken(
                tokenResponse.refreshToken,
                LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS)
            )
            ?: RefreshTokenEntity.create(
                userId,
                tokenResponse.refreshToken,
                REFRESH_TOKEN_VALIDITY_DAYS
            )

        refreshTokenRepository.save(refreshTokenEntity)
        return tokenResponse
    }

    companion object {
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 30L
    }
}