package com.snurdy.backend.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("*"),
    val allowCredentials: Boolean = true,
    val maxAgeSeconds: Long = 3600
)
