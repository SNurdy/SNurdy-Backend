package com.snurdy.backend.domain.admin.auth.service

import com.snurdy.backend.domain.admin.audit.entity.AdminAuditAction
import com.snurdy.backend.domain.admin.audit.entity.AdminAuditTarget
import com.snurdy.backend.domain.admin.audit.service.AdminAuditLogService
import com.snurdy.backend.domain.admin.auth.dto.AdminTokenResponse
import com.snurdy.backend.domain.admin.auth.dto.toResponse
import com.snurdy.backend.domain.admin.auth.repository.AdminUserRepository
import com.snurdy.backend.domain.auth.service.AccessTokenBlacklistService
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAuthService(
    private val adminUserRepository: AdminUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val rateLimiter: AdminLoginRateLimiter,
    private val auditLogService: AdminAuditLogService,
    private val accessTokenBlacklistService: AccessTokenBlacklistService
) {

    @Transactional
    fun login(
        email: String,
        password: String,
        ipAddress: String? = null,
        userAgent: String? = null
    ): AdminTokenResponse {
        val normalizedEmail = email.trim().lowercase()
        rateLimiter.ensureAllowed(normalizedEmail, ipAddress)

        val admin = adminUserRepository.findByEmail(normalizedEmail)
        if (admin == null || !passwordEncoder.matches(password, admin.password)) {
            rateLimiter.recordFailure(normalizedEmail, ipAddress)
            auditLogService.write(
                actorRole = ACTOR_ROLE,
                action = AdminAuditAction.ADMIN_LOGIN_FAILED,
                targetTable = AdminAuditTarget.ADMIN_USERS,
                targetPk = normalizedEmail.ifBlank { UNKNOWN_TARGET },
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            throw ApiException(AuthError.LOGIN_FAILED)
        }

        rateLimiter.recordSuccess(normalizedEmail, ipAddress)
        auditLogService.write(
            actorRole = ACTOR_ROLE,
            action = AdminAuditAction.ADMIN_LOGIN_SUCCESS,
            targetTable = AdminAuditTarget.ADMIN_USERS,
            targetPk = admin.id.toString(),
            ipAddress = ipAddress,
            userAgent = userAgent
        )

        val accessToken = jwtTokenProvider.createAdminAccessToken(admin.id)
        return AdminTokenResponse(
            accessToken = accessToken,
            accessTokenExpiresIn = jwtTokenProvider.adminAccessTokenExpiration(),
            admin = admin.toResponse()
        )
    }

    fun logout(accessToken: String) {
        accessTokenBlacklistService.blacklistAccessToken(accessToken)
    }

    companion object {
        private const val ACTOR_ROLE = "ADMIN"
        private const val UNKNOWN_TARGET = "unknown"
    }
}
