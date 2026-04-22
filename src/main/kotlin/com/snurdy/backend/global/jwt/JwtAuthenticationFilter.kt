package com.snurdy.backend.global.jwt

import com.snurdy.backend.domain.admin.auth.repository.AdminUserRepository
import com.snurdy.backend.domain.auth.service.AccessTokenBlacklistService
import com.snurdy.backend.domain.user.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val adminUserRepository: AdminUserRepository,
    private val accessTokenBlacklistService: AccessTokenBlacklistService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)
        if (accessTokenBlacklistService.isBlacklisted(token)) {
            SecurityContextHolder.clearContext()
            filterChain.doFilter(request, response)
            return
        }

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response)
            return
        }

        val tokenType = jwtTokenProvider.getTokenType(token)
        if (tokenType != TokenType.ACCESS && tokenType != TokenType.ADMIN) {
            filterChain.doFilter(request, response)
            return
        }

        val subjectId = jwtTokenProvider.getUserIdFromToken(token)
        val authority = resolveAuthority(tokenType, subjectId)
        if (authority == null) {
            SecurityContextHolder.clearContext()
            filterChain.doFilter(request, response)
            return
        }

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(subjectId, null, listOf(authority))

        filterChain.doFilter(request, response)
    }

    private fun resolveAuthority(tokenType: TokenType, subjectId: java.util.UUID): SimpleGrantedAuthority? =
        when (tokenType) {
            TokenType.ACCESS -> {
                val user = userRepository.findByIdOrNull(subjectId)
                if (user == null || user.isDeleted()) null
                else SimpleGrantedAuthority("ROLE_${user.role.name}")
            }
            TokenType.ADMIN -> {
                val admin = adminUserRepository.findByIdOrNull(subjectId)
                if (admin == null) null else SimpleGrantedAuthority("ROLE_ADMIN")
            }
            else -> null
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
