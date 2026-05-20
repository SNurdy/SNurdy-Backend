package com.snurdy.backend.domain.admin.auth.controller

import com.snurdy.backend.domain.admin.auth.dto.AdminLoginRequest
import com.snurdy.backend.domain.admin.auth.dto.AdminTokenResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Admin-Auth", description = "관리자 인증 API")
@RequestMapping("/v1/admin/auth")
interface AdminAuthApi {

    @Operation(summary = "관리자 로그인 (이메일 + 비밀번호)")
    @ApiSuccessCode("AUTH_S001")
    @ApiErrorCodes("AUTH_E010", "AUTH_E011")
    @SecurityRequirements
    @PostMapping("/login")
    fun login(@RequestBody @Valid request: AdminLoginRequest): ApiResponse<AdminTokenResponse>

    @Operation(summary = "관리자 로그아웃 (access token 블랙리스트 등록)")
    @ApiSuccessCode("AUTH_S002")
    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") accessToken: String): ApiResponse<Unit>
}
