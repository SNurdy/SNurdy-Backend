package com.snurdy.backend.domain.auth.controller

import com.snurdy.backend.domain.auth.dto.AppleLoginRequest
import com.snurdy.backend.domain.auth.dto.GoogleLoginRequest
import com.snurdy.backend.domain.auth.dto.KakaoLoginRequest
import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "Auth API")
@RequestMapping("/v1/auth")
interface AuthApi {

    @Operation(summary = "카카오 로그인", description = "카카오 SDK에서 받은 accessToken으로 로그인합니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
        SwaggerApiResponse(responseCode = "502", description = "카카오 API 오류")
    )
    @SecurityRequirements
    @PostMapping("/login/kakao")
    fun kakaoLogin(
        @RequestBody @Valid request: KakaoLoginRequest
    ): ApiResponse<TokenResponse>

    @Operation(summary = "애플 로그인", description = "Apple identityToken으로 로그인합니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않은 identityToken"),
        SwaggerApiResponse(responseCode = "502", description = "애플 API 오류")
    )
    @SecurityRequirements
    @PostMapping("/login/apple")
    fun appleLogin(
        @RequestBody @Valid request: AppleLoginRequest
    ): ApiResponse<TokenResponse>

    @Operation(summary = "구글 로그인", description = "Google id_token으로 로그인합니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않은 id_token"),
        SwaggerApiResponse(responseCode = "502", description = "구글 API 오류")
    )
    @SecurityRequirements
    @PostMapping("/login/google")
    fun googleLogin(
        @RequestBody @Valid request: GoogleLoginRequest
    ): ApiResponse<TokenResponse>

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "재발급 성공"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    )
    @SecurityRequirements
    @PostMapping("/reissue")
    fun reissue(
        @RequestHeader("Authorization-Refresh") refreshToken: String
    ): ApiResponse<TokenResponse>

    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하여 로그아웃합니다.")
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공")
    )
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization-Refresh") refreshToken: String
    ): ApiResponse<Unit>
}