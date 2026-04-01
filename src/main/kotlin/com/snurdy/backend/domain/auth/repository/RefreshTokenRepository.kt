package com.snurdy.backend.domain.auth.repository

import com.snurdy.backend.domain.auth.model.RefreshToken
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.TimeUnit

@Repository
class RefreshTokenRepository(
    private val redisTemplate: StringRedisTemplate
) {
    fun save(refreshToken: RefreshToken) {
        val key = generateKey(refreshToken.userId)
        redisTemplate.opsForValue().set(
            key,
            refreshToken.token,
            refreshToken.ttlSeconds,
            TimeUnit.SECONDS
        )
    }

    fun findByUserId(userId: UUID): String? =
        redisTemplate.opsForValue().get(generateKey(userId))

    fun deleteByUserId(userId: UUID) {
        redisTemplate.delete(generateKey(userId))
    }

    fun existsByUserId(userId: UUID): Boolean =
        redisTemplate.hasKey(generateKey(userId))

    private fun generateKey(userId: UUID): String = "$KEY_PREFIX$userId"

    companion object {
        private const val KEY_PREFIX = "refresh_token:"
    }
}