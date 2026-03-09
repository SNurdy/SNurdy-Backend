package com.snurdy.backend.global.jwt

import com.snurdy.backend.domain.admin.auth.repository.AdminUserRepository
import com.snurdy.backend.domain.user.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val adminUserRepository: AdminUserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val role = jwtTokenProvider.getRoleFromToken(token)

            val authentication = when (role) {
                "ADMIN" -> {
                    val admin = adminUserRepository.findById(userId).orElse(null)
                    if (admin != null) {
                        UsernamePasswordAuthenticationToken(
                            admin.id,
                            null,
                            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
                        )
                    } else null
                }
                else -> {
                    val user = userRepository.findById(userId).orElse(null)
                    if (user != null) {
                        UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                        )
                    } else null
                }
            }

            if (authentication != null) {
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        return if (bearerToken?.startsWith(BEARER_PREFIX) == true) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else null
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}