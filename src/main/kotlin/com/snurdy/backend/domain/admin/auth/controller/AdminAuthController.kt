package com.snurdy.backend.domain.admin.auth.controller

import com.snurdy.backend.domain.admin.auth.dto.AdminLoginRequest
import com.snurdy.backend.domain.admin.auth.dto.AdminTokenResponse
import com.snurdy.backend.domain.admin.auth.service.AdminAuthService
import com.snurdy.backend.global.constants.AuthSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.extension.extractBearerToken
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminAuthController(
    private val adminAuthService: AdminAuthService
) : AdminAuthApi {

    override fun login(request: AdminLoginRequest): ApiResponse<AdminTokenResponse> {
        val httpRequest = currentRequest()
        return ApiResponse.success(
            AuthSuccess.LOGIN,
            adminAuthService.login(
                email = request.email,
                password = request.password,
                ipAddress = clientIp(httpRequest),
                userAgent = httpRequest?.getHeader("User-Agent")
            )
        )
    }

    override fun logout(accessToken: String): ApiResponse<Unit> {
        adminAuthService.logout(accessToken.extractBearerToken())
        return ApiResponse.success(AuthSuccess.LOGOUT)
    }

    private fun currentRequest(): HttpServletRequest? =
        (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
                as? org.springframework.web.context.request.ServletRequestAttributes)
            ?.request

    private fun clientIp(request: HttpServletRequest?): String? {
        if (request == null) return null
        val forwarded = request.getHeader("X-Forwarded-For")
        // Caddy는 실제 클라이언트 IP를 X-Forwarded-For 맨 뒤에 append한다.
        // 앞쪽 값은 클라이언트가 위조 가능하므로 마지막 값만 신뢰한다.
        return forwarded
            ?.split(",")
            ?.map { it.trim() }
            ?.lastOrNull { it.isNotBlank() }
            ?: request.remoteAddr
    }
}
