package com.snurdy.backend.global.config

import com.snurdy.backend.global.exception.SecurityExceptionHandler
import com.snurdy.backend.global.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsProperties: CorsProperties
) {

    // Chain 1: 정적 자원 / 공개 페이지 / 인프라 헬스체크 / 스웨거.
    // JWT 필터 미탑재, 세션 없음. CMS 정적 HTML/CSS/JS 도 여기로.
    @Bean
    @Order(1)
    fun publicAssetsFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(
                OrRequestMatcher(
                    AntPathRequestMatcher("/"),
                    AntPathRequestMatcher("/health"),
                    AntPathRequestMatcher("/v1/health"),
                    AntPathRequestMatcher("/actuator/health"),
                    AntPathRequestMatcher("/actuator/prometheus"),
                    AntPathRequestMatcher("/swagger-ui/**"),
                    AntPathRequestMatcher("/swagger-resources/**"),
                    AntPathRequestMatcher("/v3/api-docs/**"),
                    AntPathRequestMatcher("/webjars/**"),
                    AntPathRequestMatcher("/swagger"),
                    AntPathRequestMatcher("/admin/**", HttpMethod.GET.name())
                )
            )
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .headers { headers ->
                // CMS 페이지 clickjacking 방어
                headers.addHeaderWriter(XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.DENY))
            }

        return http.build()
    }

    // Chain 2: 인증 및 버전 체크 API (비로그인 허용).
    @Bean
    @Order(2)
    fun publicApiFilterChain(http: HttpSecurity, securityExceptionHandler: SecurityExceptionHandler): SecurityFilterChain {
        val corsSource = corsConfigurationSource()
        http.securityMatcher(
            OrRequestMatcher(
                AntPathRequestMatcher("/v1/auth/**"),
                AntPathRequestMatcher("/v1/admin/auth/**"),
                AntPathRequestMatcher("/v1/app/version/**")
            )
        )
        http {
            cors { configurationSource = corsSource }
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }

            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }

            exceptionHandling {
                authenticationEntryPoint = securityExceptionHandler
                accessDeniedHandler = securityExceptionHandler
            }

            // admin logout은 블랙리스트 등록을 위해 JWT 필터가 먼저 돌며 blacklist 체크를 해야 함
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }

        return http.build()
    }

    // Chain 3: 보호된 애플리케이션 API. JWT 인증 필수.
    // /internal 은 denyAll (프로파일로 load 여부가 결정되지만 런타임 2중 차단).
    @Bean
    @Order(3)
    fun protectedApiFilterChain(http: HttpSecurity, securityExceptionHandler: SecurityExceptionHandler): SecurityFilterChain {
        val corsSource = corsConfigurationSource()
        http {
            cors { configurationSource = corsSource }
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }

            authorizeHttpRequests {
                // 관리자 API
                authorize("/v1/admin/**", hasRole("ADMIN"))
                // 마이그레이션
                authorize("/v1/migration/**", hasRole("USER"))
                // Actuator 비공개 엔드포인트 (env, beans, loggers 등)
                authorize("/actuator/**", hasRole("ADMIN"))
                // Internal 은 완전 차단
                authorize("/internal/**", denyAll)
                // 그 외 인증 필요
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
            allowedOrigins = corsProperties.allowedOrigins
            allowedMethods = corsProperties.allowedMethods
            allowedHeaders = corsProperties.allowedHeaders
            allowCredentials = corsProperties.allowCredentials
            maxAge = corsProperties.maxAgeSeconds
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
