package com.snurdy.backend.domain.auth.controller

import com.snurdy.backend.domain.auth.dto.AuthDto
import com.snurdy.backend.domain.auth.service.AuthService
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.global.common.constants.SuccessCode
import com.snurdy.backend.global.common.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService
) : AuthApi {

    override fun kakaoLogin(
        request: AuthDto.KakaoLoginRequest
    ): ApiResponse<AuthDto.TokenResponse> {
        val token = authService.kakaoLogin(request.accessToken)
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, token)
    }

    override fun appleLogin(
        request: AuthDto.AppleLoginRequest
    ): ApiResponse<AuthDto.TokenResponse> {
        val token = authService.appleLogin(request.identityToken, request.user)
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, token)
    }

    override fun reissue(refreshToken: String): ApiResponse<AuthDto.TokenResponse> {
        val token = authService.reissue(refreshToken.replace("Bearer ", ""))
        return ApiResponse.success(SuccessCode.REISSUE_SUCCESS, token)
    }

    override fun logout(refreshToken: String): ApiResponse<Unit> {
        authService.logout(refreshToken.replace("Bearer ", ""))
        return ApiResponse.success(SuccessCode.LOGOUT_SUCCESS)
    }
}