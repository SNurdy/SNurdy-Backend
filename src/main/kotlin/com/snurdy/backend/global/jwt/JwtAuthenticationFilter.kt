package com.snurdy.backend.global.jwt

import com.snurdy.backend.domain.auth.service.AccessTokenBlacklistService
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
    private val accessTokenBlacklistService: AccessTokenBlacklistService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response)
            return
        }

        val tokenType = jwtTokenProvider.getTokenType(token)
        if (tokenType != TokenType.ACCESS) {
            filterChain.doFilter(request, response)
            return
        }

        if (accessTokenBlacklistService.isBlacklisted(token)) {
            SecurityContextHolder.clearContext()
            filterChain.doFilter(request, response)
            return
        }

        val subjectId = jwtTokenProvider.getUserIdFromToken(token)
        val role = jwtTokenProvider.getRoleFromToken(token)

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(subjectId, null, listOf(SimpleGrantedAuthority("ROLE_$role")))

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
