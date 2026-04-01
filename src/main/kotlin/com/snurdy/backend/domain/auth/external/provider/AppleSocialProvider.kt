package com.snurdy.backend.domain.auth.external.provider

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.snurdy.backend.domain.auth.dto.AppleUser
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.exception.ApiException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class AppleSocialProvider(
    private val applePublicKeyProvider: ApplePublicKeyProvider,
    @Value("\${oauth.apple.client-id}") private val appleClientId: String
) {
    private val mapper = jacksonObjectMapper()

    fun verifyAndGetUserInfo(identityToken: String, appleUser: AppleUser?): SocialUserInfo {
        val claims = verifyIdentityToken(identityToken)

        return SocialUserInfo(
            socialId = claims.subject,
            email = (claims["email"] as? String) ?: appleUser?.email,
            name = appleUser?.fullName,
            nickname = null
        )
    }

    private fun verifyIdentityToken(identityToken: String): Claims {
        val kid = extractHeader(identityToken)["kid"] as? String
            ?: throw ApiException(AuthError.INVALID_TOKEN)

        return Jwts.parser()
            .verifyWith(applePublicKeyProvider.getPublicKey(kid))
            .requireIssuer(APPLE_ISSUER)
            .requireAudience(appleClientId)
            .build()
            .parseSignedClaims(identityToken)
            .payload
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
        private const val APPLE_ISSUER = "https://appleid.apple.com"
    }
}