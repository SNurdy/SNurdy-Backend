package com.snurdy.backend.global.security.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.*

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {
    private lateinit var key: Key

    @PostConstruct
    protected fun init() {
        val keyBytes = jwtProperties.secretKey.toByteArray(StandardCharsets.UTF_8)
        this.key = Keys.hmacShaKeyFor(keyBytes)
    }

    // Access Token 생성
    fun createAccessToken(userId: UUID, role: String = "USER"): String {
        val claims = Jwts.claims().setSubject(userId.toString())
        claims["role"] = role

        val now = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + jwtProperties.accessTokenExpiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    // Refresh Token 생성
    fun createRefreshToken(): String {
        val now = Date()
        return Jwts.builder()
            .setIssuedAt(now)
            .setExpiration(Date(now.time + jwtProperties.refreshTokenExpiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    // 토큰에서 UUID 추출
    fun getUserId(token: String): String {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
    }

    // 토큰 유효성 검증
    fun validateToken(token: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            !claims.body.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }
}