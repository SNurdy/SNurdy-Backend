package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.api.auth.client.SocialProvider
import com.snurdy.backend.api.auth.dto.response.TokenResponse
import com.snurdy.backend.domain.auth.entity.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.User
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.common.constants.ErrorType
import com.snurdy.backend.global.exception.CustomException
import com.snurdy.backend.global.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class AuthService(
    private val socialProviders: List<SocialProvider>,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun socialLogin(socialType: SocialType, authorizationCode: String): TokenResponse {
        val provider = socialProviders.find { it.getSocialType() == socialType }
            ?: throw CustomException(ErrorType.INVALID_SOCIAL_TYPE)

        val accessToken = provider.getAccessToken(authorizationCode)

        val socialUserInfo = provider.getSocialUserInfo(accessToken)

        val user = userRepository.findBySocialTypeAndSocialId(socialType, socialUserInfo.socialId)
            ?: userRepository.save(User.create(socialUserInfo, socialType))

        val tokenResponse = jwtTokenProvider.createToken(user.id)

        refreshTokenRepository.deleteByUserId(user.id)
        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                token = tokenResponse.refreshToken,
                expiryDate = LocalDateTime.now().plusDays(30)
            )
        )

        return tokenResponse
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): TokenResponse {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: throw CustomException(ErrorType.INVALID_REFRESH_TOKEN)

        if (storedToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw CustomException(ErrorType.EXPIRED_TOKEN)
        }

        val newTokenResponse = jwtTokenProvider.createToken(storedToken.userId)

        refreshTokenRepository.deleteByUserId(storedToken.userId)
        refreshTokenRepository.save(
            RefreshToken(
                userId = storedToken.userId,
                token = newTokenResponse.refreshToken,
                expiryDate = LocalDateTime.now().plusDays(30)
            )
        )

        return newTokenResponse
    }
}