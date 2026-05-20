package com.snurdy.backend.domain.user.controller

import com.snurdy.backend.domain.user.dto.MarketingConsentRequest
import com.snurdy.backend.domain.user.dto.MarketingConsentResponse
import com.snurdy.backend.domain.user.dto.UserProfileResponse
import com.snurdy.backend.domain.user.dto.UserProfileUpdateRequest
import com.snurdy.backend.domain.user.service.UserMeService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserProfileController(
    private val userMeService: UserMeService
) : UserProfileApi {

    override fun getProfile(userId: UUID): ApiResponse<UserProfileResponse> =
        ApiResponse.success(UserSuccess.GET_PROFILE, userMeService.getProfile(userId))

    override fun updateProfile(
        userId: UUID,
        request: UserProfileUpdateRequest
    ): ApiResponse<UserProfileResponse> =
        ApiResponse.success(UserSuccess.UPDATE_PROFILE, userMeService.updateProfile(userId, request))

    override fun updateMarketingConsent(
        userId: UUID,
        request: MarketingConsentRequest
    ): ApiResponse<MarketingConsentResponse> =
        ApiResponse.success(UserSuccess.UPDATE_MARKETING_CONSENT, userMeService.updateMarketingConsent(userId, request.agreed))
}
