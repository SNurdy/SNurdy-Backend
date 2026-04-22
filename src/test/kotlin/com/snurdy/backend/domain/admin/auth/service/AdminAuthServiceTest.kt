package com.snurdy.backend.domain.admin.auth.service

import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import com.snurdy.backend.domain.admin.audit.repository.AdminAuditLogRepository
import com.snurdy.backend.domain.admin.auth.entity.AdminUserEntity
import com.snurdy.backend.domain.admin.auth.repository.AdminUserRepository
import com.snurdy.backend.domain.auth.service.AccessTokenBlacklistService
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class AdminAuthServiceTest {

    private lateinit var adminUserRepository: AdminUserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var rateLimiter: AdminLoginRateLimiter
    private lateinit var auditLogRepository: AdminAuditLogRepository
    private lateinit var accessTokenBlacklistService: AccessTokenBlacklistService

    private lateinit var service: AdminAuthService

    @BeforeEach
    fun setUp() {
        adminUserRepository = mockk()
        passwordEncoder = mockk()
        jwtTokenProvider = mockk()
        rateLimiter = mockk(relaxed = true)
        auditLogRepository = mockk(relaxed = true)
        accessTokenBlacklistService = mockk(relaxed = true)

        service = AdminAuthService(
            adminUserRepository = adminUserRepository,
            passwordEncoder = passwordEncoder,
            jwtTokenProvider = jwtTokenProvider,
            rateLimiter = rateLimiter,
            auditLogRepository = auditLogRepository,
            accessTokenBlacklistService = accessTokenBlacklistService
        )
    }

    @Test
    fun `login - 비밀번호가 틀리면 실패 카운터 증가 + 실패 감사 로그 기록 후 LOGIN_FAILED`() {
        val admin = adminUser()
        every { adminUserRepository.findByEmail(EMAIL) } returns admin
        every { passwordEncoder.matches(PASSWORD, admin.password) } returns false

        val auditCaptured = slot<AdminAuditLogEntity>()
        every { auditLogRepository.save(capture(auditCaptured)) } answers { auditCaptured.captured }

        val ex = assertThrows(ApiException::class.java) {
            service.login(EMAIL, PASSWORD, ipAddress = IP, userAgent = UA)
        }

        assertEquals(AuthError.LOGIN_FAILED, ex.error)
        verify(exactly = 1) { rateLimiter.ensureAllowed(EMAIL, IP) }
        verify(exactly = 1) { rateLimiter.recordFailure(EMAIL, IP) }
        verify(exactly = 0) { rateLimiter.recordSuccess(any(), any()) }
        assertEquals("ADMIN_LOGIN_FAILED", auditCaptured.captured.action)
        assertEquals(EMAIL, auditCaptured.captured.targetPk)
        assertEquals(IP, auditCaptured.captured.ipAddress)
    }

    @Test
    fun `login - 존재하지 않는 이메일이면 admin 존재 여부를 노출하지 않고 LOGIN_FAILED`() {
        every { adminUserRepository.findByEmail(EMAIL) } returns null
        val auditCaptured = slot<AdminAuditLogEntity>()
        every { auditLogRepository.save(capture(auditCaptured)) } answers { auditCaptured.captured }

        val ex = assertThrows(ApiException::class.java) {
            service.login(EMAIL, PASSWORD, IP, UA)
        }

        assertEquals(AuthError.LOGIN_FAILED, ex.error)
        verify(exactly = 1) { rateLimiter.recordFailure(EMAIL, IP) }
        assertEquals("ADMIN_LOGIN_FAILED", auditCaptured.captured.action)
    }

    @Test
    fun `login - rate limiter가 차단하면 더 이상 비밀번호 검사를 하지 않는다`() {
        every { rateLimiter.ensureAllowed(EMAIL, IP) } throws ApiException(AuthError.LOGIN_RATE_LIMITED)

        val ex = assertThrows(ApiException::class.java) {
            service.login(EMAIL, PASSWORD, IP, UA)
        }

        assertEquals(AuthError.LOGIN_RATE_LIMITED, ex.error)
        verify(exactly = 0) { adminUserRepository.findByEmail(any()) }
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `login - 성공하면 실패 카운터 초기화, 성공 감사 로그, admin 토큰 발급`() {
        val admin = adminUser()
        every { adminUserRepository.findByEmail(EMAIL) } returns admin
        every { passwordEncoder.matches(PASSWORD, admin.password) } returns true
        every { jwtTokenProvider.createAdminAccessToken(admin.id) } returns ACCESS_TOKEN
        every { jwtTokenProvider.accessTokenExpiration() } returns EXPIRES

        val auditCaptured = slot<AdminAuditLogEntity>()
        every { auditLogRepository.save(capture(auditCaptured)) } answers { auditCaptured.captured }

        val response = service.login(EMAIL, PASSWORD, IP, UA)

        assertEquals(ACCESS_TOKEN, response.accessToken)
        assertEquals(EXPIRES, response.accessTokenExpiresIn)
        assertEquals(admin.id, response.admin.id)
        verify(exactly = 1) { rateLimiter.recordSuccess(EMAIL, IP) }
        verify(exactly = 0) { rateLimiter.recordFailure(any(), any()) }
        assertEquals("ADMIN_LOGIN_SUCCESS", auditCaptured.captured.action)
        assertEquals(admin.id.toString(), auditCaptured.captured.targetPk)
    }

    @Test
    fun `logout - access token을 블랙리스트에 등록한다`() {
        service.logout(ACCESS_TOKEN)
        verify(exactly = 1) { accessTokenBlacklistService.blacklistAccessToken(ACCESS_TOKEN) }
    }

    private fun adminUser(): AdminUserEntity = AdminUserEntity(
        id = ADMIN_ID,
        email = EMAIL,
        password = "hashed",
        name = "Admin"
    )

    companion object {
        private const val EMAIL = "admin@snurdy.local"
        private const val PASSWORD = "p@ssw0rd"
        private const val IP = "203.0.113.1"
        private const val UA = "JUnit"
        private const val ACCESS_TOKEN = "admin-access-token"
        private const val EXPIRES = 3_600_000L
        private val ADMIN_ID: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    }
}
