package com.snurdy.backend.domain.admin.auth.service

import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AdminLoginRateLimiter(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun ensureAllowed(normalizedEmail: String, ipAddress: String?) {
        val emailCount = currentCount(emailKey(normalizedEmail))
        val ipCount = ipAddress?.let { currentCount(ipKey(it)) } ?: 0
        if (emailCount >= MAX_FAILURES_PER_EMAIL || ipCount >= MAX_FAILURES_PER_IP) {
            throw ApiException(AuthError.LOGIN_RATE_LIMITED)
        }
    }

    fun recordFailure(normalizedEmail: String, ipAddress: String?) {
        increment(emailKey(normalizedEmail))
        ipAddress?.let { increment(ipKey(it)) }
    }

    fun recordSuccess(normalizedEmail: String, ipAddress: String?) {
        runCatching { redisTemplate.delete(emailKey(normalizedEmail)) }
            .onFailure { log.warn("Failed to clear admin login failure counter for email", it) }
        ipAddress?.let {
            runCatching { redisTemplate.delete(ipKey(it)) }
                .onFailure { e -> log.warn("Failed to clear admin login failure counter for ip", e) }
        }
    }

    private fun currentCount(key: String): Long =
        try {
            redisTemplate.opsForValue().get(key)?.toLongOrNull() ?: 0L
        } catch (e: DataAccessException) {
            log.error("Redis lookup failed during admin login rate-limit check; failing closed", e)
            throw ApiException(CommonError.INTERNAL_ERROR, cause = e)
        }

    private fun increment(key: String) {
        try {
            val newValue = redisTemplate.opsForValue().increment(key) ?: return
            if (newValue == 1L) {
                redisTemplate.expire(key, WINDOW)
            }
        } catch (e: DataAccessException) {
            log.error("Redis write failed during admin login failure record; failing closed", e)
            throw ApiException(CommonError.INTERNAL_ERROR, cause = e)
        }
    }

    private fun emailKey(email: String) = "$KEY_PREFIX:email:$email"
    private fun ipKey(ip: String) = "$KEY_PREFIX:ip:$ip"

    companion object {
        private const val KEY_PREFIX = "admin_login_failure"
        private const val MAX_FAILURES_PER_EMAIL = 5L
        private const val MAX_FAILURES_PER_IP = 30L
        private val WINDOW: Duration = Duration.ofMinutes(15)
    }
}
