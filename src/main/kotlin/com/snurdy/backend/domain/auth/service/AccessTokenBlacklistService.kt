package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.global.jwt.JwtTokenProvider
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
    fun blacklistAccessToken(token: String?) {
        if (token.isNullOrBlank()) return
        if (!jwtTokenProvider.validateToken(token)) return
        if (jwtTokenProvider.getTokenType(token) != ACCESS_TOKEN_TYPE) return

        val expiration = runCatching { jwtTokenProvider.parseClaims(token).expiration }.getOrNull() ?: return
        val ttlMillis = expiration.time - Instant.now().toEpochMilli()
        if (ttlMillis <= 0) return

        runCatching {
            redisTemplate.opsForValue().set(
                generateKey(token),
                BLACKLISTED_MARK,
                ttlMillis,
                TimeUnit.MILLISECONDS
            )
        }
    }

    fun isBlacklisted(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return runCatching { redisTemplate.hasKey(generateKey(token)) == true }
            .getOrDefault(false)
    }

    private fun generateKey(token: String): String = "$KEY_PREFIX${sha256Hex(token)}"

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_PREFIX = "access_token_blacklist:"
        private const val BLACKLISTED_MARK = "1"
        private const val ACCESS_TOKEN_TYPE = "ACCESS"
    }
}
