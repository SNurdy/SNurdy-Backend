package com.snurdy.backend.domain.user.preference.controller

import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultCardCountRequest
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultCardCountResponse
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultTestTypesRequest
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultTestTypesResponse
import com.snurdy.backend.domain.user.preference.dto.UpdateSelectedGlossaryRequest
import com.snurdy.backend.domain.user.preference.dto.UpdateSelectedGlossaryResponse
import com.snurdy.backend.domain.user.preference.dto.UserPreferenceResponse
import com.snurdy.backend.domain.user.preference.service.UserPreferenceService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserPreferenceController(
    private val userPreferenceService: UserPreferenceService
) : UserPreferenceApi {

    override fun getPreferences(userId: UUID): ApiResponse<UserPreferenceResponse> =
        ApiResponse.success(UserSuccess.GET_PREFERENCES, userPreferenceService.getPreferenceResponse(userId))

    override fun updateDefaultTestTypes(
        userId: UUID,
        request: UpdateDefaultTestTypesRequest
    ): ApiResponse<UpdateDefaultTestTypesResponse> =
        ApiResponse.success(
            UserSuccess.UPDATE_DEFAULT_TEST_TYPES,
            userPreferenceService.updateDefaultTestTypes(userId, request.defaultTestTypes ?: emptyList())
        )

    override fun updateSelectedGlossary(
        userId: UUID,
        request: UpdateSelectedGlossaryRequest
    ): ApiResponse<UpdateSelectedGlossaryResponse> =
        ApiResponse.success(
            UserSuccess.UPDATE_SELECTED_GLOSSARY,
            userPreferenceService.updateSelectedGlossary(userId, request.glossaryId!!)
        )

    override fun updateDefaultCardCount(
        userId: UUID,
        request: UpdateDefaultCardCountRequest
    ): ApiResponse<UpdateDefaultCardCountResponse> =
        ApiResponse.success(
            UserSuccess.UPDATE_DEFAULT_CARD_COUNT,
            userPreferenceService.updateDefaultCardCount(userId, request.count!!)
        )
}
