package com.snurdy.backend.global.config

import com.snurdy.backend.global.exception.SecurityExceptionHandler
import com.snurdy.backend.global.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    @Order(2)
    fun filterChain(http: HttpSecurity, securityExceptionHandler: SecurityExceptionHandler): SecurityFilterChain {
        http {
            cors { configurationSource = corsConfigurationSource() }
            csrf { disable() }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            authorizeHttpRequests {
                // 헬스체크
                authorize("/", permitAll)
                authorize("/health", permitAll)
                authorize("/v1/health", permitAll)

                // 일반 사용자 인증
                authorize("/v1/auth/**", permitAll)

                // 관리자 로그인
                authorize("/v1/admin/auth/**", permitAll)

                // 관리자 APIㄷㄷㄷㄷ
                authorize("/v1/admin/**", hasRole("ADMIN"))

                // CMS 정적 페이지
                authorize("/admin/**", permitAll)

                // Swagger
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-resources/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/webjars/**", permitAll)

                // Internal
                authorize("/internal/**", denyAll)

                authorize(anyRequest, authenticated)
            }

            exceptionHandling {
                authenticationEntryPoint = securityExceptionHandler
                accessDeniedHandler = securityExceptionHandler
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:5173", "http://localhost:8081")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}