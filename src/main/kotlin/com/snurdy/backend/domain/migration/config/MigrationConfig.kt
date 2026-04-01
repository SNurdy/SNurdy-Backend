// MigrationConfig.kt
package com.snurdy.backend.domain.migration.config

import com.snurdy.backend.global.constants.MigrationError
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Configuration
class MigrationConfig {

    @Bean
    fun migrationPayloadSizeFilter(): FilterRegistrationBean<Filter> {
        return FilterRegistrationBean<Filter>().apply {
            filter = Filter { request, response, chain ->
                val httpRequest = request as HttpServletRequest
                val httpResponse = response as HttpServletResponse

                if (httpRequest.requestURI.contains("/migration") &&
                    httpRequest.requestURI.contains("/upload")
                ) {
                    val contentLength = httpRequest.contentLengthLong
                    if (contentLength > MAX_PAYLOAD_BYTES) {
                        httpResponse.sendError(
                            MigrationError.MIGRATION_PAYLOAD_TOO_LARGE.status.value(),
                            MigrationError.MIGRATION_PAYLOAD_TOO_LARGE.message
                        )
                        return@Filter
                    }
                }
                chain.doFilter(request, response)
            }
            addUrlPatterns("/v1/migration/*")
            order = 1
        }
    }

    companion object {
        private const val MAX_PAYLOAD_BYTES = 10 * 1024 * 1024L
    }
}