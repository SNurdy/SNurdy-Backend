package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.domain.auth.dto.AppleUser
import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.auth.external.provider.AppleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.GoogleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.KakaoSocialProvider
import com.snurdy.backend.domain.auth.model.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtTokenProvider
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthService(
    private val kakaoSocialProvider: KakaoSocialProvider,
    private val appleSocialProvider: AppleSocialProvider,
    private val googleSocialProvider: GoogleSocialProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun kakaoLogin(accessToken: String): TokenResponse =
        processLogin(kakaoSocialProvider.getUserInfo(accessToken), SocialType.KAKAO)

    @Transactional
    fun appleLogin(identityToken: String, appleUser: AppleUser?): TokenResponse =
        processLogin(appleSocialProvider.verifyAndGetUserInfo(identityToken, appleUser), SocialType.APPLE)

    @Transactional
    fun googleLogin(idToken: String): TokenResponse =
        processLogin(googleSocialProvider.verifyAndGetUserInfo(idToken), SocialType.GOOGLE)

    fun reissue(refreshToken: String): TokenResponse {
        if (!jwtTokenProvider.validateToken(refreshToken)) throw ApiException(AuthError.INVALID_TOKEN)
        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        validateRefreshToken(userId, refreshToken)
        return userRepository.findByIdOrNull(userId)
            ?.let(::issueTokens)
            ?: throw ApiException(UserError.NOT_FOUND)
    }

    fun logout(refreshToken: String) {
        if (!jwtTokenProvider.validateToken(refreshToken)) throw ApiException(AuthError.INVALID_TOKEN)
        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        validateRefreshToken(userId, refreshToken)
        refreshTokenRepository.deleteByUserId(userId)
    }

    fun issueTokens(user: UserEntity): TokenResponse =
        jwtTokenProvider.createToken(user).also { response ->
            RefreshToken.create(
                userId = user.id,
                token = response.refreshToken,
                ttlDays = REFRESH_TOKEN_VALIDITY_DAYS
            ).let { refreshTokenRepository.save(it) }
        }

    private fun processLogin(userInfo: SocialUserInfo, socialType: SocialType): TokenResponse {
        val activeUser = userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(socialType, userInfo.socialId)
        if (activeUser != null) {
            return issueTokens(activeUser)
        }

        val deletedUser = userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNotNull(socialType, userInfo.socialId)
        if (deletedUser != null && deletedUser.isRecoverable()) {
            val tokenResponse = jwtTokenProvider.createToken(deletedUser)
            return tokenResponse.copy(
                accountRecoverable = true,
                recoverableUntil = deletedUser.deletedAt?.plusSeconds(UserEntity.RECOVERY_PERIOD_DAYS * 86400)
            )
        }

        return createNewUser(userInfo, socialType)
    }

    private fun createNewUser(userInfo: SocialUserInfo, socialType: SocialType): TokenResponse =
        runCatching {
            userRepository.save(UserEntity.from(userInfo, socialType))
        }.recoverCatching { e ->
            if (e is DataIntegrityViolationException) {
                userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(socialType, userInfo.socialId)
                    ?: throw e
            } else throw e
        }.getOrThrow().let(::issueTokens)

    private fun validateRefreshToken(userId: UUID, refreshToken: String) {
        val savedToken = refreshTokenRepository.findByUserId(userId)
            ?: throw ApiException(AuthError.EXPIRED_TOKEN)
        if (savedToken != refreshToken) {
            refreshTokenRepository.deleteByUserId(userId)
            throw ApiException(AuthError.INVALID_TOKEN)
        }
    }

    companion object {
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 30L
    }
}