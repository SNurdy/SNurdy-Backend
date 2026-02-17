package com.snurdy.backend.internal.seed.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Profile("local", "dev")
@Configuration
class InternalSeedSecurityConfig {

    @Bean
    @Order(1)
    fun internalSeedFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/internal/**")
            csrf { disable() }
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
        }

        return http.build()
    }
}
