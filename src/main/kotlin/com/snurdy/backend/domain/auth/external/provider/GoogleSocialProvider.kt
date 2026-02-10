// src/main/kotlin/com/snurdy/backend/domain/auth/external/provider/GoogleSocialProvider.kt
package com.snurdy.backend.domain.auth.external.provider

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.exception.ApiException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class GoogleSocialProvider(
    private val googlePublicKeyProvider: GooglePublicKeyProvider,
    @Value("\${oauth.google.client-id}") private val googleClientId: String
) {
    private val mapper = jacksonObjectMapper()

    fun verifyAndGetUserInfo(idToken: String): SocialUserInfo {
        val claims = verifyIdToken(idToken)

        return SocialUserInfo(
            socialId = claims.subject,
            email = claims["email"] as? String,
            name = claims["name"] as? String,
            nickname = null
        )
    }

    private fun verifyIdToken(idToken: String): Claims {
        val kid = extractHeader(idToken)["kid"] as? String
            ?: throw ApiException(AuthError.INVALID_TOKEN)

        val claims = Jwts.parser()
            .verifyWith(googlePublicKeyProvider.getPublicKey(kid))
            .requireAudience(googleClientId)
            .build()
            .parseSignedClaims(idToken)
            .payload

        // issuer 수동 검증
        val issuer = claims.issuer
        if (issuer != GOOGLE_ISSUER && issuer != GOOGLE_ISSUER_ALT) {
            throw ApiException(AuthError.INVALID_TOKEN)
        }

        return claims
    }

    private fun extractHeader(token: String): Map<String, Any> =
        runCatching {
            token.substringBefore(".")
                .let { Base64.getUrlDecoder().decode(it) }
                .let { mapper.readValue(it, object : TypeReference<Map<String, Any>>() {}) }
        }.getOrElse {
            throw ApiException(AuthError.INVALID_TOKEN)
        }

    companion object {
        private const val GOOGLE_ISSUER = "https://accounts.google.com"
        private const val GOOGLE_ISSUER_ALT = "accounts.google.com"
    }
}