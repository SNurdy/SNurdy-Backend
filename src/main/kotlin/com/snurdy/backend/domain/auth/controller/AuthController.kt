package com.snurdy.backend.domain.auth.controller
import com.snurdy.backend.domain.auth.service.AuthService
import com.snurdy.backend.domain.auth.dto.AuthDto
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.global.common.constants.SuccessCode
import com.snurdy.backend.global.common.dto.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login/{socialType}")
    fun socialLogin(
        @PathVariable socialType: SocialType,
        @RequestBody @Valid request: AuthDto.LoginRequest
    ): ApiResponse<AuthDto.TokenResponse> {
        val token = authService.socialLogin(socialType, request.authorizationCode)
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, token)
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestHeader("Authorization-Refresh") refreshToken: String
    ): ApiResponse<AuthDto.TokenResponse> {
        val token = authService.reissue(refreshToken.replace("Bearer ", ""))
        return ApiResponse.success(SuccessCode.REISSUE_SUCCESS, token)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization-Refresh") refreshToken: String
    ): ApiResponse<Unit> {
        authService.logout(refreshToken.replace("Bearer ", ""))
        return ApiResponse.success(SuccessCode.LOGOUT_SUCCESS)
    }
}