package com.snurdy.backend.domain.auth.external.provider

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.snurdy.backend.domain.auth.dto.AuthDto
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.global.common.constants.ErrorCode
import com.snurdy.backend.global.exception.ApiException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class AppleSocialProvider(
    private val applePublicKeyProvider: ApplePublicKeyProvider
) {
    private val mapper = ObjectMapper()

    fun verifyAndGetUserInfo(
        identityToken: String,
        appleUser: AuthDto.AppleUser?
    ): SocialUserInfo {
        val claims = verifyIdentityToken(identityToken)
        val sub = claims.subject

        val nickname = appleUser?.fullName ?: "Apple User"

        return SocialUserInfo(
            socialId = sub,
            email = (claims["email"] as? String) ?: appleUser?.email,
            nickname = nickname
        )
    }

    private fun verifyIdentityToken(identityToken: String): Claims {
        val header = extractHeader(identityToken)
        val kid = header["kid"] as? String
            ?: throw ApiException(ErrorCode.INVALID_TOKEN)

        val publicKey = applePublicKeyProvider.getPublicKey(kid)

        return Jwts.parser()
            .verifyWith(publicKey)
            .requireIssuer("https://appleid.apple.com")
            .requireAudience("org.snurdy.dev")
            .build()
            .parseSignedClaims(identityToken)
            .payload
    }

    private fun extractHeader(token: String): Map<String, Any> {
        val headerPart = token.split(".")[0]
        val decoded = Base64.getUrlDecoder().decode(headerPart)

        val header: Map<String, Any> = mapper.readValue(
            decoded,
            object : TypeReference<Map<String, Any>>() {}
        )
        return header
    }
}