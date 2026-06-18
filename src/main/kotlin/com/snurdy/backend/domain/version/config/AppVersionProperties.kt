package com.snurdy.backend.domain.version.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.version")
data class AppVersionProperties(
    val latest: String,
    val minimum: String,
    val updateMessage: String? = null,
    val forceUpdateMessage: String? = null,
    val storeUrl: String? = null
)
