package com.snurdy.backend.domain.user.profile.controller

import com.snurdy.backend.domain.user.profile.dto.MarketingConsentRequest
import com.snurdy.backend.domain.user.profile.dto.MarketingConsentResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileUpdateRequest
import com.snurdy.backend.domain.user.profile.service.UserProfileService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserProfileController(
    private val userProfileService: UserProfileService
) : UserProfileApi {

    override fun getProfile(userId: UUID): ApiResponse<UserProfileResponse> =
        ApiResponse.success(UserSuccess.GET_PROFILE, userProfileService.getProfile(userId))

    override fun updateProfile(
        userId: UUID,
        request: UserProfileUpdateRequest
    ): ApiResponse<UserProfileResponse> =
        ApiResponse.success(UserSuccess.UPDATE_PROFILE, userProfileService.updateProfile(userId, request))

    override fun updateMarketingConsent(
        userId: UUID,
        request: MarketingConsentRequest
    ): ApiResponse<MarketingConsentResponse> =
        ApiResponse.success(UserSuccess.UPDATE_MARKETING_CONSENT, userProfileService.updateMarketingConsent(userId, request.agreed))
}
