package com.snurdy.backend.global.security.jwt

import com.snurdy.backend.api.auth.dto.response.TokenResponse
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {
    private lateinit var secretKey: SecretKey

    @PostConstruct
    fun init() {
        val rawKey = jwtProperties.secretKey

        require(rawKey.isNotBlank()) {
            "JWT Secret Key가 주입되지 않았습니다. 환경 변수(JWT_SECRET_KEY)를 확인하세요."
        }

        require(rawKey.length >= 32) {
            "JWT Secret Key는 보안을 위해 최소 32자 이상이어야 합니다. (현재 길이: ${rawKey.length})"
        }

        this.secretKey = Keys.hmacShaKeyFor(rawKey.toByteArray())
    }

    fun generateAccessToken(userId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.accessTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(userId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.refreshTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return UUID.fromString(claims.subject)
    }

    fun createToken(userId: UUID): TokenResponse {
        return TokenResponse(
            accessToken = generateAccessToken(userId),
            refreshToken = generateRefreshToken(userId),
            accessTokenExpiresIn = jwtProperties.accessTokenExpiration,
            refreshTokenExpiresIn = jwtProperties.refreshTokenExpiration
        )
    }
}