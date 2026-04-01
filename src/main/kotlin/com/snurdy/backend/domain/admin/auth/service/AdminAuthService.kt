package com.snurdy.backend.domain.admin.auth.service

import com.snurdy.backend.domain.admin.auth.dto.AdminInfo
import com.snurdy.backend.domain.admin.auth.dto.AdminTokenResponse
import com.snurdy.backend.domain.admin.auth.repository.AdminUserRepository
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AdminAuthService(
    private val adminUserRepository: AdminUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    fun login(email: String, password: String): AdminTokenResponse {
        val admin = adminUserRepository.findByEmail(email.trim())
            ?: throw ApiException(AuthError.LOGIN_FAILED)

        if (!passwordEncoder.matches(password, admin.password)) {
            throw ApiException(AuthError.LOGIN_FAILED)
        }

        val accessToken = jwtTokenProvider.createAdminAccessToken(admin.id)

        return AdminTokenResponse(
            accessToken = accessToken,
            accessTokenExpiresIn = jwtTokenProvider.accessTokenExpiration(),
            admin = AdminInfo(id = admin.id, email = admin.email, name = admin.name)
        )
    }
}