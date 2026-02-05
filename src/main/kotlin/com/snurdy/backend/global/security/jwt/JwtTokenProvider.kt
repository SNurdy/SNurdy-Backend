package com.snurdy.backend.global.security.jwt

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
        accessToken = generateToken(user.id, jwtProperties.accessTokenExpiration),
        refreshToken = generateToken(user.id, jwtProperties.refreshTokenExpiration),
        accessTokenExpiresIn = jwtProperties.accessTokenExpiration,
        refreshTokenExpiresIn = jwtProperties.refreshTokenExpiration,
        user = user.toUserInfo()
    )

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

    private fun generateToken(userId: UUID, expirationMillis: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + expirationMillis))
            .signWith(secretKey)
            .compact()
    }
}