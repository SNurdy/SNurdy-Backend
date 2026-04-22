package com.snurdy.backend.internal.auth.service

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.model.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.jwt.JwtTokenProvider
import org.springframework.context.annotation.Profile
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Profile("(local | dev) & !prod")
@Service
class InternalDevAuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun issueDevToken(): TokenResponse {
        val user = findOrCreateDevUser()
        val token = jwtTokenProvider.createToken(user)

        refreshTokenRepository.save(
            RefreshToken.create(
                userId = user.id,
                token = token.refreshToken,
                ttlDays = REFRESH_TOKEN_VALIDITY_DAYS
            )
        )

        return token
    }

    private fun findOrCreateDevUser(): UserEntity =
        runCatching {
            userRepository.findBySocialTypeAndSocialId(DEV_SOCIAL_TYPE, DEV_SOCIAL_ID)
                ?: userRepository.saveAndFlush(
                    UserEntity(
                        socialId = DEV_SOCIAL_ID,
                        socialType = DEV_SOCIAL_TYPE,
                        email = DEV_EMAIL,
                        name = DEV_NAME,
                        nickname = DEV_NICKNAME
                    )
                )
        }.recoverCatching { e ->
            if (e is DataIntegrityViolationException) {
                userRepository.findBySocialTypeAndSocialId(DEV_SOCIAL_TYPE, DEV_SOCIAL_ID)
                    ?: throw e
            } else {
                throw e
            }
        }.getOrThrow()

    companion object {
        private val DEV_SOCIAL_TYPE = SocialType.KAKAO
        private const val DEV_SOCIAL_ID = "internal-dev-user"
        private const val DEV_EMAIL = "internal-dev@snurdy.local"
        private const val DEV_NAME = "Internal Dev"
        private const val DEV_NICKNAME = "internal-dev"
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 30L
    }
}
