package com.snurdy.backend.global.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.concurrent.TimeUnit

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secretKey: String,
    val accessTokenExpiration: Long,
    val refreshTokenExpiration: Long
) {
    val refreshTokenValidityDays: Long
        get() = TimeUnit.MILLISECONDS.toDays(refreshTokenExpiration)
}
