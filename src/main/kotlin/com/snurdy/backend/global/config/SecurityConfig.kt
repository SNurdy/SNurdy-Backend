package com.snurdy.backend.global.config

import com.snurdy.backend.global.exception.SecurityExceptionHandler
import com.snurdy.backend.global.jwt.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
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

    // Chain 0 (dev 전용): swagger 경로는 BasicAuth 로 보호. dev 환경이 인터넷에 노출되어 있어 endpoint/스키마 외부 노출 방지
    // prod 는 springdoc 자체 비활성이라 이 chain 도 빌드 안 됨
    @Bean
    @Order(0)
    @Profile("dev")
    fun swaggerBasicAuthFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(
                OrRequestMatcher(
                    AntPathRequestMatcher("/swagger-ui/**"),
                    AntPathRequestMatcher("/swagger-resources/**"),
                    AntPathRequestMatcher("/v3/api-docs/**"),
                    AntPathRequestMatcher("/webjars/**"),
                    AntPathRequestMatcher("/swagger")
                )
            )
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().hasRole("SWAGGER") }
            .httpBasic { }

        return http.build()
    }

    @Bean
    @Profile("dev")
    fun swaggerUserDetailsService(
        @Value("\${swagger.auth.username}") username: String,
        @Value("\${swagger.auth.password}") password: String,
        passwordEncoder: PasswordEncoder
    ): UserDetailsService {
        val user = User.withUsername(username)
            .password(passwordEncoder.encode(password))
            .roles("SWAGGER")
            .build()
        return InMemoryUserDetailsManager(user)
    }

    // Chain 1: 정적 자원 / 공개 페이지 / 인프라 헬스체크 / 스웨거.
    // JWT 필터 미탑재, 세션 없음. CMS 정적 HTML/CSS/JS 도 여기로.
    // actuator prometheus 는 내부 Prometheus 스크래핑 전용으로 permitAll 로 개방하고 외부는 nginx 가 deny all 로 차단
    // dev profile 에서는 swagger 경로가 Chain 0 에서 먼저 매칭되어 BasicAuth 거침
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
                    AntPathRequestMatcher("/actuator/prometheus", HttpMethod.GET.name()),
                    AntPathRequestMatcher("/swagger-ui/**"),
                    AntPathRequestMatcher("/swagger-resources/**"),
                    AntPathRequestMatcher("/v3/api-docs/**"),
                    AntPathRequestMatcher("/webjars/**"),
                    AntPathRequestMatcher("/swagger")
                )
            )
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().permitAll() }

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

            // logout은 블랙리스트 등록을 위해 JWT 필터가 먼저 돌며 blacklist 체크를 해야 함
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
                // 마이그레이션
                authorize("/v1/migration/**", hasRole("USER"))
                // Actuator 비공개 엔드포인트는 발급 가능한 권한이 없으므로 완전 차단. health prometheus 는 Chain1 permitAll
                authorize("/actuator/**", denyAll)
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
