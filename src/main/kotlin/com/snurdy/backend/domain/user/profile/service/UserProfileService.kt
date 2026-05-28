package com.snurdy.backend.domain.user.profile.service

import com.snurdy.backend.domain.user.profile.dto.MarketingConsentResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileUpdateRequest
import com.snurdy.backend.domain.user.profile.dto.toMarketingConsentResponse
import com.snurdy.backend.domain.user.profile.dto.toProfileResponse
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): UserProfileResponse {
        val user = findActiveUser(userId)
        return user.toProfileResponse()
    }

    @Transactional
    fun updateProfile(userId: UUID, request: UserProfileUpdateRequest): UserProfileResponse {
        val user = findActiveUser(userId)

        request.nickname?.let { user.updateNickname(it) }
        request.profileImageUrl?.let { user.updateProfileImageUrl(it) }

        return user.toProfileResponse()
    }

    @Transactional
    fun updateMarketingConsent(userId: UUID, agreed: Boolean): MarketingConsentResponse {
        val user = findActiveUser(userId)
        user.setMarketingConsent(agreed, clock.instant())

        return user.toMarketingConsentResponse()
    }

    private fun findActiveUser(userId: UUID): UserEntity {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ApiException(UserError.ACCOUNT_NOT_FOUND)
        if (user.isDeleted()) throw ApiException(UserError.ALREADY_DELETED)
        return user
    }
}
