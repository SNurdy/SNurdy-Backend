package com.snurdy.backend.global.jwt

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.dto.toUserInfo
import com.snurdy.backend.domain.user.entity.UserEntity
import io.jsonwebtoken.Claims
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

    fun createAdminAccessToken(adminId: UUID): String =
        generateToken(adminId, "ADMIN", jwtProperties.accessTokenExpiration, tokenType = "admin")

    fun validateToken(token: String): Boolean = runCatching {
        parseClaims(token)
    }.isSuccess

    fun getUserIdFromToken(token: String): UUID =
        parseClaims(token)
            .subject
            .let(UUID::fromString)

    fun getTokenType(token: String): String =
        parseClaims(token)
            .get("type", String::class.java) ?: "user"

    fun getRoleFromToken(token: String): String =
        parseClaims(token)
            .get("role", String::class.java) ?: "USER"

    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun accessTokenExpiration(): Long = jwtProperties.accessTokenExpiration

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