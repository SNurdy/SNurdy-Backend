package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtTokenProvider
import com.snurdy.backend.global.jwt.TokenType
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class AccessTokenBlacklistService(
    private val redisTemplate: StringRedisTemplate,
    private val jwtTokenProvider: JwtTokenProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun blacklistAccessToken(token: String?) {
        if (token.isNullOrBlank()) return
        if (!jwtTokenProvider.validateToken(token)) return
        if (jwtTokenProvider.getTokenType(token) !in BLACKLISTABLE_TYPES) return

        val expiration = runCatching { jwtTokenProvider.parseClaims(token).expiration }.getOrNull() ?: return
        val ttlMillis = expiration.time - Instant.now().toEpochMilli()
        if (ttlMillis <= 0) return

        try {
            redisTemplate.opsForValue().set(
                generateKey(token),
                BLACKLISTED_MARK,
                ttlMillis,
                TimeUnit.MILLISECONDS
            )
        } catch (e: DataAccessException) {
            log.error("Access Token 블랙리스트 등록 실패 — 자격증명 재사용 방지를 위해 로그아웃 요청을 거부합니다", e)
            throw ApiException(CommonError.SERVICE_UNAVAILABLE, cause = e)
        }
    }

    fun isBlacklisted(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return try {
            redisTemplate.hasKey(generateKey(token)) == true
        } catch (e: DataAccessException) {
            log.error("Access Token 블랙리스트 조회 실패 — 보안 원칙에 따라 인증을 차단합니다", e)
            throw ApiException(CommonError.SERVICE_UNAVAILABLE, cause = e)
        }
    }

    private fun generateKey(token: String): String = "$KEY_PREFIX${sha256Hex(token)}"

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_PREFIX = "access_token_blacklist:"
        private const val BLACKLISTED_MARK = "1"
        private val BLACKLISTABLE_TYPES = setOf(TokenType.ACCESS, TokenType.ADMIN)
    }
}
