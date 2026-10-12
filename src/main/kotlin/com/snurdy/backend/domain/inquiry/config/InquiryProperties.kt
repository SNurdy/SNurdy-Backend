package com.snurdy.backend.domain.inquiry.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "inquiry")
data class InquiryProperties(
    val mail: Mail,
    val rateLimit: RateLimit = RateLimit()
) {
    data class Mail(
        val queueName: String,
        val sender: String,
        val senderName: String,
        val receiver: String,
        val subjectPrefix: String
    )

    data class RateLimit(
        val cooldownSeconds: Long = DEFAULT_COOLDOWN_SECONDS,
        val dailyLimit: Long = DEFAULT_DAILY_LIMIT
    )

    companion object {
        private const val DEFAULT_COOLDOWN_SECONDS: Long = 30
        private const val DEFAULT_DAILY_LIMIT: Long = 10
    }
}