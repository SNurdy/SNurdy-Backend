package com.snurdy.backend.domain.admin.auth.service

import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.domain.admin.auth.dto.AdminInfo
import com.snurdy.backend.domain.admin.auth.dto.AdminTokenResponse
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
    private val auditLogRepository: AdminAuditLogRepository,
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

        val admin = adminUserRepository.findByEmail(email.trim())
        if (admin == null || !passwordEncoder.matches(password, admin.password)) {
            rateLimiter.recordFailure(normalizedEmail, ipAddress)
            writeAudit(
                action = ACTION_LOGIN_FAILED,
                targetPk = normalizedEmail.ifBlank { UNKNOWN_TARGET },
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            throw ApiException(AuthError.LOGIN_FAILED)
        }

        rateLimiter.recordSuccess(normalizedEmail, ipAddress)
        writeAudit(
            action = ACTION_LOGIN_SUCCESS,
            targetPk = admin.id.toString(),
            ipAddress = ipAddress,
            userAgent = userAgent
        )

        val accessToken = jwtTokenProvider.createAdminAccessToken(admin.id)
        return AdminTokenResponse(
            accessToken = accessToken,
            accessTokenExpiresIn = jwtTokenProvider.accessTokenExpiration(),
            admin = AdminInfo(id = admin.id, email = admin.email, name = admin.name)
        )
    }

    fun logout(accessToken: String) {
        accessTokenBlacklistService.blacklistAccessToken(accessToken)
    }

    private fun writeAudit(action: String, targetPk: String, ipAddress: String?, userAgent: String?) {
        auditLogRepository.save(
            AdminAuditLogEntity(
                actorRole = ACTOR_ROLE,
                action = action,
                targetTable = TARGET_TABLE,
                targetPk = targetPk.take(MAX_PK_LENGTH),
                ipAddress = ipAddress?.take(MAX_IP_LENGTH),
                userAgent = userAgent?.take(MAX_UA_LENGTH)
            )
        )
    }

    companion object {
        private const val ACTOR_ROLE = "ADMIN"
        private const val TARGET_TABLE = "admin_users"
        private const val ACTION_LOGIN_SUCCESS = "ADMIN_LOGIN_SUCCESS"
        private const val ACTION_LOGIN_FAILED = "ADMIN_LOGIN_FAILED"
        private const val UNKNOWN_TARGET = "unknown"
        private const val MAX_PK_LENGTH = 100
        private const val MAX_IP_LENGTH = 45
        private const val MAX_UA_LENGTH = 255
    }
}
