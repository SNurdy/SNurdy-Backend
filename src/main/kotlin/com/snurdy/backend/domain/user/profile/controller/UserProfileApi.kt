package com.snurdy.backend.domain.user.profile.controller

import com.snurdy.backend.domain.user.profile.dto.MarketingConsentRequest
import com.snurdy.backend.domain.user.profile.dto.MarketingConsentResponse
import com.snurdy.backend.domain.user.profile.dto.UpdateDefaultTestTypeRequest
import com.snurdy.backend.domain.user.profile.dto.UpdateDefaultTestTypeResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileUpdateRequest
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(name = "User-Profile", description = "v2 사용자 프로필 API")
@RequestMapping("/v2/users/me")
interface UserProfileApi {

    @Operation(summary = "본인 프로필 조회")
    @ApiSuccessCode("USER_S011")
    @ApiErrorCodes("USER_E010", "USER_E005")
    @GetMapping
    fun getProfile(@CurrentUserId userId: UUID): ApiResponse<UserProfileResponse>

    @Operation(summary = "본인 프로필 부분 수정 (닉네임, 프로필 이미지)")
    @ApiSuccessCode("USER_S012")
    @ApiErrorCodes("USER_E010", "USER_E005")
    @PatchMapping
    fun updateProfile(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: UserProfileUpdateRequest
    ): ApiResponse<UserProfileResponse>

    @Operation(summary = "마케팅 수신 동의 변경")
    @ApiSuccessCode("USER_S013")
    @ApiErrorCodes("USER_E010", "USER_E005")
    @PutMapping("/marketing-consent")
    fun updateMarketingConsent(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: MarketingConsentRequest
    ): ApiResponse<MarketingConsentResponse>

    @Operation(summary = "기본 테스트 유형 변경 (다음부터 묻지 않기)")
    @ApiSuccessCode("USER_S030")
    @ApiErrorCodes("USER_E010", "USER_E005")
    @PutMapping("/preferences/test-type")
    fun updateDefaultTestType(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: UpdateDefaultTestTypeRequest
    ): ApiResponse<UpdateDefaultTestTypeResponse>
}
