package com.snurdy.backend.domain.user.preference.controller

import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultTestTypeRequest
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultTestTypeResponse
import com.snurdy.backend.domain.user.preference.dto.UpdateSelectedGlossaryRequest
import com.snurdy.backend.domain.user.preference.dto.UpdateSelectedGlossaryResponse
import com.snurdy.backend.domain.user.preference.service.UserPreferenceService
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserPreferenceController(
    private val userPreferenceService: UserPreferenceService
) : UserPreferenceApi {

    override fun updateDefaultTestType(
        userId: UUID,
        request: UpdateDefaultTestTypeRequest
    ): ApiResponse<UpdateDefaultTestTypeResponse> =
        ApiResponse.success(
            UserSuccess.UPDATE_DEFAULT_TEST_TYPE,
            userPreferenceService.updateDefaultTestType(userId, request.defaultTestType)
        )

    override fun updateSelectedGlossary(
        userId: UUID,
        request: UpdateSelectedGlossaryRequest
    ): ApiResponse<UpdateSelectedGlossaryResponse> =
        ApiResponse.success(
            UserSuccess.UPDATE_SELECTED_GLOSSARY,
            userPreferenceService.updateSelectedGlossary(userId, request.glossaryId!!)
        )
}
