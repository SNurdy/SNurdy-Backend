package com.snurdy.backend.domain.user.service

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.auth.service.AuthService
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserAccountService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val authService: AuthService
) {

    @Transactional
    fun withdraw(userId: UUID) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ApiException(UserError.NOT_FOUND)
        if (user.isDeleted()) throw ApiException(UserError.ALREADY_DELETED)

        user.softDelete()
        refreshTokenRepository.deleteByUserId(userId)
    }

    @Transactional
    fun recover(userId: UUID): TokenResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ApiException(UserError.NOT_FOUND)
        if (!user.isDeleted()) throw ApiException(UserError.NOT_DELETED)
        if (!user.isRecoverable()) throw ApiException(UserError.RECOVERY_EXPIRED)

        user.recover()
        return authService.issueTokens(user)
    }
}