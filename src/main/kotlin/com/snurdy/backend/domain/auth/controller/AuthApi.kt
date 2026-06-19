package com.snurdy.backend.domain.auth.controller

import com.snurdy.backend.domain.auth.dto.AppleLoginRequest
import com.snurdy.backend.domain.auth.dto.GoogleLoginRequest
import com.snurdy.backend.domain.auth.dto.KakaoLoginRequest
import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "인증 API")
@RequestMapping("/v1/auth")
interface AuthApi {

    @Operation(summary = "카카오 로그인")
    @ApiSuccessCode("AUTH_S001")
    @ApiErrorCodes("AUTH_E001")
    @SecurityRequirements
    @PostMapping("/login/kakao")
    fun kakaoLogin(@RequestBody @Valid request: KakaoLoginRequest): ApiResponse<TokenResponse>

    @Operation(summary = "애플 로그인")
    @ApiSuccessCode("AUTH_S001")
    @ApiErrorCodes("AUTH_E001")
    @SecurityRequirements
    @PostMapping("/login/apple")
    fun appleLogin(@RequestBody @Valid request: AppleLoginRequest): ApiResponse<TokenResponse>

    @Operation(summary = "구글 로그인")
    @ApiSuccessCode("AUTH_S001")
    @ApiErrorCodes("AUTH_E001")
    @SecurityRequirements
    @PostMapping("/login/google")
    fun googleLogin(@RequestBody @Valid request: GoogleLoginRequest): ApiResponse<TokenResponse>

    @Operation(summary = "토큰 재발급")
    @ApiSuccessCode("AUTH_S004")
    @ApiErrorCodes("AUTH_E001", "AUTH_E002", "USER_E001")
    @SecurityRequirements
    @PostMapping("/reissue")
    fun reissue(@RequestHeader("Authorization-Refresh") refreshToken: String): ApiResponse<TokenResponse>

    @Operation(summary = "로그아웃")
    @ApiSuccessCode("AUTH_S002")
    @ApiErrorCodes("AUTH_E001", "AUTH_E002")
    @SecurityRequirements
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization-Refresh") refreshToken: String,
        @RequestHeader("Authorization") accessToken: String
    ): ApiResponse<Unit>
}
