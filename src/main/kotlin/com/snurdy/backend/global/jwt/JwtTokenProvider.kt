package com.snurdy.backend.global.jwt

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.dto.toUserInfo
import com.snurdy.backend.domain.user.entity.UserEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {
    private val secretKey = Keys.hmacShaKeyFor(
        jwtProperties.secretKey.toByteArray(StandardCharsets.UTF_8)
    )

    fun createToken(user: UserEntity) = TokenResponse(
        accessToken = generateToken(user.id, user.role.name, jwtProperties.accessTokenExpiration),
        refreshToken = generateToken(user.id, user.role.name, jwtProperties.refreshTokenExpiration),
        accessTokenExpiresIn = jwtProperties.accessTokenExpiration,
        refreshTokenExpiresIn = jwtProperties.refreshTokenExpiration,
        user = user.toUserInfo()
    )

    fun createAdminToken(adminId: UUID): Pair<String, String> {
        val accessToken = generateToken(adminId, "ADMIN", jwtProperties.accessTokenExpiration)
        val refreshToken = generateToken(adminId, "ADMIN", jwtProperties.refreshTokenExpiration)
        return accessToken to refreshToken
    }

    fun validateToken(token: String): Boolean = runCatching {
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
    }.isSuccess

    fun getUserIdFromToken(token: String): UUID =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
            .let(UUID::fromString)

    fun getRoleFromToken(token: String): String =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .get("role", String::class.java) ?: "USER"

    fun accessTokenExpiration(): Long = jwtProperties.accessTokenExpiration
    fun refreshTokenExpiration(): Long = jwtProperties.refreshTokenExpiration

    private fun generateToken(userId: UUID, role: String, expirationMillis: Long, tokenType: String = "user"): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .claim("type", tokenType)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMillis))
            .signWith(secretKey)
            .compact()
    }
}