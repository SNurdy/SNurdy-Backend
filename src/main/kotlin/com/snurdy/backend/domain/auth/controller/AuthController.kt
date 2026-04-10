package com.snurdy.backend.domain.auth.controller

import com.snurdy.backend.domain.auth.dto.AppleLoginRequest
import com.snurdy.backend.domain.auth.dto.GoogleLoginRequest
import com.snurdy.backend.domain.auth.dto.KakaoLoginRequest
import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.service.AuthService
import com.snurdy.backend.global.constants.AuthSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.extension.extractBearerToken
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService
) : AuthApi {

    override fun kakaoLogin(request: KakaoLoginRequest): ApiResponse<TokenResponse> =
        ApiResponse.success(AuthSuccess.LOGIN, authService.kakaoLogin(request.accessToken))

    override fun appleLogin(request: AppleLoginRequest): ApiResponse<TokenResponse> =
        ApiResponse.success(AuthSuccess.LOGIN, authService.appleLogin(request.identityToken, request.user))

    override fun googleLogin(request: GoogleLoginRequest): ApiResponse<TokenResponse> =
        ApiResponse.success(AuthSuccess.LOGIN, authService.googleLogin(request.idToken))

    override fun reissue(refreshToken: String): ApiResponse<TokenResponse> =
        ApiResponse.success(AuthSuccess.REISSUE, authService.reissue(refreshToken.extractBearerToken()))

    override fun logout(refreshToken: String, accessToken: String): ApiResponse<Unit> {
        authService.logout(
            refreshToken = refreshToken.extractBearerToken(),
            accessToken = accessToken.extractBearerToken()
        )
        return ApiResponse.success(AuthSuccess.LOGOUT)
    }
}
