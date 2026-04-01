package com.snurdy.backend.domain.admin.auth.controller

import com.snurdy.backend.domain.admin.auth.dto.AdminLoginRequest
import com.snurdy.backend.domain.admin.auth.dto.AdminTokenResponse
import com.snurdy.backend.domain.admin.auth.service.AdminAuthService
import com.snurdy.backend.global.constants.AuthSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminAuthController(
    private val adminAuthService: AdminAuthService
) : AdminAuthApi {

    override fun login(request: AdminLoginRequest): ApiResponse<AdminTokenResponse> =
        ApiResponse.success(AuthSuccess.LOGIN, adminAuthService.login(request.email, request.password))
}