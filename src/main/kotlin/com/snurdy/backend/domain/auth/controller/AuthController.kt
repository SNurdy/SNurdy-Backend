package com.snurdy.backend.domain.auth.controller
import com.snurdy.backend.domain.auth.service.AuthService
import com.snurdy.backend.domain.auth.dto.AuthDto
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.global.common.constants.SuccessCode
import com.snurdy.backend.global.common.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "Auth API")
@RestController
@RequestMapping("/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @Operation(
        summary = "소셜 로그인",
        description = "카카오 또는 애플 소셜 로그인을 수행합니다."
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
        SwaggerApiResponse(responseCode = "400", description = "잘못된 요청"),
        SwaggerApiResponse(responseCode = "502", description = "소셜 API 오류")
    )
    @PostMapping("/login/{socialType}")
    fun socialLogin(
        @PathVariable socialType: SocialType,
        @RequestBody @Valid request: AuthDto.LoginRequest
    ): ApiResponse<AuthDto.TokenResponse> {
        val token = authService.socialLogin(socialType, request.authorizationCode)
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, token)
    }

    @Operation(
        summary = "토큰 재발급",
        description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다."
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "재발급 성공"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    )
    @PostMapping("/reissue")
    fun reissue(
        @RequestHeader("Authorization-Refresh") refreshToken: String
    ): ApiResponse<AuthDto.TokenResponse> {
        val token = authService.reissue(refreshToken.replace("Bearer ", ""))
        return ApiResponse.success(SuccessCode.REISSUE_SUCCESS, token)
    }

    @Operation(
        summary = "로그아웃",
        description = "Refresh Token을 무효화하여 로그아웃합니다."
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공")
    )
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization-Refresh") refreshToken: String
    ): ApiResponse<Unit> {
        authService.logout(refreshToken.replace("Bearer ", ""))
        return ApiResponse.success(SuccessCode.LOGOUT_SUCCESS)
    }
}