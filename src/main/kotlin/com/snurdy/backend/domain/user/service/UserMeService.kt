package com.snurdy.backend.domain.user.service

import com.snurdy.backend.domain.user.dto.MarketingConsentResponse
import com.snurdy.backend.domain.user.dto.UserProfileResponse
import com.snurdy.backend.domain.user.dto.UserProfileUpdateRequest
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class UserMeService(
    private val userRepository: UserRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): UserProfileResponse {
        val user = findActiveUser(userId)
        return UserProfileResponse.from(user)
    }

    @Transactional
    fun updateProfile(userId: UUID, request: UserProfileUpdateRequest): UserProfileResponse {
        val user = findActiveUser(userId)

        request.nickname?.let { user.updateNickname(it) }
        request.profileImageUrl?.let { user.updateProfileImageUrl(it) }

        return UserProfileResponse.from(user)
    }

    @Transactional
    fun updateMarketingConsent(userId: UUID, agreed: Boolean): MarketingConsentResponse {
        val user = findActiveUser(userId)
        user.setMarketingConsent(agreed, clock.instant())

        return MarketingConsentResponse(
            marketingConsent = user.marketingConsent,
            marketingConsentAt = user.marketingConsentAt
        )
    }

    private fun findActiveUser(userId: UUID): UserEntity {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ApiException(CommonError.UNAUTHORIZED)
        if (user.isDeleted()) throw ApiException(UserError.ALREADY_DELETED)
        return user
    }
}
