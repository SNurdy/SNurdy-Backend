package com.snurdy.backend.domain.user.preference.controller

import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultCardCountRequest
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultCardCountResponse
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultTestTypesRequest
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultTestTypesResponse
import com.snurdy.backend.domain.user.preference.dto.UpdateSelectedGlossaryRequest
import com.snurdy.backend.domain.user.preference.dto.UpdateSelectedGlossaryResponse
import com.snurdy.backend.domain.user.preference.dto.UserPreferenceResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(name = "User Preference", description = "v2 사용자 학습 옵션 API (기본 테스트 유형 리스트, 선택된 단어장)")
@RequestMapping("/v2/users/me/preferences")
interface UserPreferenceApi {

    @Operation(summary = "사용자 학습 옵션 조회")
    @ApiSuccessCode("USER_S032")
    @ApiErrorCodes("USER_E010")
    @GetMapping
    fun getPreferences(@CurrentUserId userId: UUID): ApiResponse<UserPreferenceResponse>

    @Operation(summary = "기본 테스트 유형 리스트 변경")
    @ApiSuccessCode("USER_S030")
    @ApiErrorCodes("USER_E010", "USER_E005")
    @PutMapping("/test-types")
    fun updateDefaultTestTypes(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: UpdateDefaultTestTypesRequest
    ): ApiResponse<UpdateDefaultTestTypesResponse>

    @Operation(summary = "선택된 단어장 변경")
    @ApiSuccessCode("USER_S031")
    @ApiErrorCodes("USER_E010", "DICTIONARY_E001")
    @PutMapping("/selected-glossary")
    fun updateSelectedGlossary(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: UpdateSelectedGlossaryRequest
    ): ApiResponse<UpdateSelectedGlossaryResponse>

    @Operation(summary = "한 세션 학습 카드 수 변경")
    @ApiSuccessCode("USER_S033")
    @ApiErrorCodes("USER_E010", "USER_E044")
    @PutMapping("/default-card-count")
    fun updateDefaultCardCount(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: UpdateDefaultCardCountRequest
    ): ApiResponse<UpdateDefaultCardCountResponse>
}
