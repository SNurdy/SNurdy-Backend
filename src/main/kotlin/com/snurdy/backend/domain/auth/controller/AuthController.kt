package com.snurdy.backend.domain.auth.controller

import com.snurdy.backend.domain.auth.dto.AppleLoginRequest
import com.snurdy.backend.domain.auth.dto.KakaoLoginRequest
import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.service.AuthService
import com.snurdy.backend.global.common.constants.SuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService
) : AuthApi {

    override fun kakaoLogin(request: KakaoLoginRequest): ApiResponse<TokenResponse> =
        authService.kakaoLogin(request.accessToken)
            .let { ApiResponse.success(SuccessCode.LOGIN_SUCCESS, it) }

    override fun appleLogin(request: AppleLoginRequest): ApiResponse<TokenResponse> =
        authService.appleLogin(request.identityToken, request.user)
            .let { ApiResponse.success(SuccessCode.LOGIN_SUCCESS, it) }

    override fun reissue(refreshToken: String): ApiResponse<TokenResponse> =
        refreshToken.extractToken()
            .let { authService.reissue(it) }
            .let { ApiResponse.success(SuccessCode.REISSUE_SUCCESS, it) }

    override fun logout(refreshToken: String): ApiResponse<Unit> {
        authService.logout(refreshToken.extractToken())
        return ApiResponse.success(SuccessCode.LOGOUT_SUCCESS)
    }

    private fun String.extractToken(): String =
        removePrefix("Bearer ").trim()
}