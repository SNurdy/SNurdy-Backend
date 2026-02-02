package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.domain.auth.dto.AuthDto
import com.snurdy.backend.domain.auth.entity.RefreshTokenEntity
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.auth.external.provider.*;
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.common.constants.ErrorCode
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class AuthService(
    private val kakaoSocialProvider: KakaoSocialProvider,
    private val appleSocialProvider: AppleSocialProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun kakaoLogin(accessToken: String): AuthDto.TokenResponse {
        val userInfo = kakaoSocialProvider.getUserInfo(accessToken)

        val user = userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, userInfo.socialId)
            ?: userRepository.save(UserEntity.from(userInfo, SocialType.KAKAO))

        return issueTokens(user.id)
    }

    @Transactional
    fun appleLogin(identityToken: String, appleUser: AuthDto.AppleUser?): AuthDto.TokenResponse {
        val userInfo = appleSocialProvider.verifyAndGetUserInfo(identityToken, appleUser)

        val user = userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, userInfo.socialId)
            ?: userRepository.save(UserEntity.from(userInfo, SocialType.APPLE))

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

    fun logout(refreshToken: String) {
        runCatching {
            if (!jwtTokenProvider.validateToken(refreshToken)) return@runCatching
            val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
            refreshTokenRepository.deleteByUserId(userId)
        }
    }

    private fun validateRefreshToken(userId: UUID, refreshToken: String) {
        val savedToken = refreshTokenRepository.findByUserId(userId)?.token
            ?: throw ApiException(ErrorCode.EXPIRED_TOKEN)

        if (savedToken != refreshToken) {
            refreshTokenRepository.deleteByUserId(userId)
            throw ApiException(ErrorCode.INVALID_TOKEN)
        }
    }

    private fun issueTokens(userId: UUID): AuthDto.TokenResponse {
        val tokenResponse = jwtTokenProvider.createToken(userId)

        refreshTokenRepository.deleteByUserId(userId)
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = userId,
                token = tokenResponse.refreshToken,
                expiryDate = LocalDateTime.now().plusDays(30)
            )
        )
        return tokenResponse
    }
}