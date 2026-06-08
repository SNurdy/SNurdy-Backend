package com.snurdy.backend.domain.user.profile.controller

import com.snurdy.backend.domain.user.profile.dto.MarketingConsentRequest
import com.snurdy.backend.domain.user.profile.dto.MarketingConsentResponse
import com.snurdy.backend.domain.user.profile.dto.ProfileImageUploadResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileUpdateRequest
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Tag(name = "User Profile", description = "v2 사용자 프로필 API")
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

    @Operation(summary = "프로필 이미지 업로드 (S3, jpg/png 5MB 이하)")
    @ApiSuccessCode("PROFILE_IMAGE_S001")
    @ApiErrorCodes("USER_E010", "USER_E005", "PROFILE_IMAGE_E001", "PROFILE_IMAGE_E002", "PROFILE_IMAGE_E003")
    @PostMapping("/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfileImage(
        @CurrentUserId userId: UUID,
        @RequestPart("image") image: MultipartFile?
    ): ApiResponse<ProfileImageUploadResponse>
}
