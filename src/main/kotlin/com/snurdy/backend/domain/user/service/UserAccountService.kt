package com.snurdy.backend.domain.user.service

import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.auth.service.AccessTokenBlacklistService
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Service
class UserAccountService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val accessTokenBlacklistService: AccessTokenBlacklistService
) {

    @Transactional
    fun withdraw(userId: UUID, accessToken: String) {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ApiException(UserError.NOT_FOUND)
        if (user.isDeleted()) throw ApiException(UserError.ALREADY_DELETED)

        user.softDelete()
        refreshTokenRepository.deleteByUserId(userId)

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    accessTokenBlacklistService.blacklistAccessToken(accessToken)
                }
            }
        )
    }
}
