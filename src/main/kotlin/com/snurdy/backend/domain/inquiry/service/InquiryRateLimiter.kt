package com.snurdy.backend.domain.inquiry.service

import com.snurdy.backend.domain.inquiry.config.InquiryProperties
import com.snurdy.backend.global.constants.APP_ZONE_ID
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.InquiryError
import com.snurdy.backend.global.exception.ApiException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@Component
class InquiryRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val inquiryProperties: InquiryProperties,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun checkMailSendingAllowed(userId: UUID) {
        try {
            checkCooldown(userId)
            checkDailyLimit(userId)
        } catch (e: DataAccessException) {
            log.error("문의 작성 횟수 제한 확인 실패", e)
            throw ApiException(CommonError.SERVICE_UNAVAILABLE, cause = e)
        }
    }

    private fun checkCooldown(userId: UUID) {
        val acquired = redisTemplate.opsForValue().setIfAbsent(
            "$COOLDOWN_KEY_PREFIX$userId",
            MARK,
            Duration.ofSeconds(inquiryProperties.rateLimit.cooldownSeconds)
        )
        if (acquired != true) {
            throw ApiException(InquiryError.TOO_MANY_INQUIRIES)
        }
    }

    private fun checkDailyLimit(userId: UUID) {
        val today = LocalDate.now(clock.withZone(APP_ZONE_ID))
        val key = "$DAILY_KEY_PREFIX$userId:$today"
        val count = redisTemplate.opsForValue().increment(key) ?: 0L
        // INCR 과 EXPIRE 사이에 서버가 죽어도 키가 영구히 남지 않도록 매번 만료를 다시 건다
        redisTemplate.expire(key, DAILY_KEY_TTL)
        if (count > inquiryProperties.rateLimit.dailyLimit) {
            throw ApiException(InquiryError.TOO_MANY_INQUIRIES)
        }
    }

    companion object {
        private const val COOLDOWN_KEY_PREFIX = "inquiry:cooldown:"
        private const val DAILY_KEY_PREFIX = "inquiry:daily:"
        private const val MARK = "1"
        private val DAILY_KEY_TTL: Duration = Duration.ofDays(2)
    }
}