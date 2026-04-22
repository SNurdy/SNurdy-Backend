package com.snurdy.backend.global.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class ProfileGuardConfig(
    private val environment: Environment
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun guardActiveProfiles() {
        val active = environment.activeProfiles.map { it.lowercase() }.toSet()
        if (active.isEmpty()) {
            log.warn("No SPRING_PROFILES_ACTIVE set; defaulting to {}", environment.defaultProfiles.toList())
            return
        }

        val unsafePairs = UNSAFE_COEXISTING_PROFILES.filter { (a, b) -> a in active && b in active }
        if (unsafePairs.isNotEmpty()) {
            val msg = unsafePairs.joinToString { (a, b) -> "[$a + $b]" }
            throw IllegalStateException(
                "Unsafe Spring profile combination detected: $msg. Active profiles=$active. " +
                        "Production must NOT coexist with non-production profiles."
            )
        }
        log.info("Active Spring profiles validated: {}", active)
    }

    companion object {
        private val UNSAFE_COEXISTING_PROFILES = listOf(
            "prod" to "dev",
            "prod" to "local",
            "prod" to "test"
        )
    }
}
