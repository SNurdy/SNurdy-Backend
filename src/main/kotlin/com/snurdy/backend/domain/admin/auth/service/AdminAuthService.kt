package com.snurdy.backend.domain.admin.auth.service

import com.snurdy.backend.domain.admin.auth.dto.AdminInfo
import com.snurdy.backend.domain.admin.auth.dto.AdminTokenResponse
import com.snurdy.backend.domain.admin.auth.repository.AdminUserRepository
import com.snurdy.backend.domain.auth.model.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AdminAuthService(
    private val adminUserRepository: AdminUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    fun login(email: String, password: String): AdminTokenResponse {
        val admin = adminUserRepository.findByEmail(email.trim())
            ?: throw ApiException(AuthError.LOGIN_FAILED)

        if (!passwordEncoder.matches(password, admin.password)) {
            throw ApiException(AuthError.LOGIN_FAILED)
        }

        val (accessToken, _) = jwtTokenProvider.createAdminToken(admin.id)

        return AdminTokenResponse(
            accessToken = accessToken,
            refreshToken = "",
            accessTokenExpiresIn = jwtTokenProvider.accessTokenExpiration(),
            refreshTokenExpiresIn = 0,
            admin = AdminInfo(id = admin.id, email = admin.email, name = admin.name)
        )
    }

    companion object {
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 30L
    }
}