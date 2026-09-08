package com.snurdy.backend.global.jwt

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

    fun createTokenPair(userId: UUID, role: String) = TokenPair(
        accessToken = generateToken(userId, role, jwtProperties.accessTokenExpiration, TokenType.ACCESS),
        refreshToken = generateToken(userId, role, jwtProperties.refreshTokenExpiration, TokenType.REFRESH),
        accessTokenExpiresIn = jwtProperties.accessTokenExpiration,
        refreshTokenExpiresIn = jwtProperties.refreshTokenExpiration
    )

    fun validateToken(token: String): Boolean = runCatching {
        parseClaims(token)
    }.isSuccess

    fun getUserIdFromToken(token: String): UUID =
        parseClaims(token)
            .subject
            .let(UUID::fromString)

    fun getTokenType(token: String): TokenType? =
        TokenType.fromClaim(parseClaims(token).get(CLAIM_TYPE, String::class.java))

    fun getRoleFromToken(token: String): String =
        parseClaims(token)
            .get(CLAIM_ROLE, String::class.java) ?: "USER"

    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun accessTokenExpiration(): Long = jwtProperties.accessTokenExpiration

    private fun generateToken(userId: UUID, role: String, expirationMillis: Long, tokenType: TokenType): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_ROLE, role)
            .claim(CLAIM_TYPE, tokenType.claimValue)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMillis))
            .signWith(secretKey)
            .compact()
    }

    companion object {
        private const val CLAIM_ROLE = "role"
        private const val CLAIM_TYPE = "type"
    }
}
