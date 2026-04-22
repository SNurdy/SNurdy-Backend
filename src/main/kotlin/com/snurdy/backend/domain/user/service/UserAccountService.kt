package com.snurdy.backend.domain.user.service

import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.auth.service.AccessTokenBlacklistService
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtTokenProvider
import com.snurdy.backend.global.jwt.TokenType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserAccountService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val accessTokenBlacklistService: AccessTokenBlacklistService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun withdraw(userId: UUID, accessToken: String? = null) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ApiException(UserError.NOT_FOUND)
        if (user.isDeleted()) throw ApiException(UserError.ALREADY_DELETED)

        if (!accessToken.isNullOrBlank()) {
            verifyTokenOwnership(accessToken, userId)
        }

        user.softDelete()
        refreshTokenRepository.deleteByUserId(userId)
        accessTokenBlacklistService.blacklistAccessToken(accessToken)
    }

    private fun verifyTokenOwnership(accessToken: String, expectedUserId: UUID) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw ApiException(AuthError.INVALID_TOKEN)
        }
        if (jwtTokenProvider.getTokenType(accessToken) != TokenType.ACCESS) {
            throw ApiException(AuthError.INVALID_TOKEN)
        }
        if (jwtTokenProvider.getUserIdFromToken(accessToken) != expectedUserId) {
            throw ApiException(AuthError.TOKEN_OWNER_MISMATCH)
        }
    }
}
