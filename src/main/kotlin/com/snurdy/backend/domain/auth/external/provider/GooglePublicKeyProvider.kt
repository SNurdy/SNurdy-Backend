package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.client.GoogleAuthClient
import com.snurdy.backend.domain.auth.external.dto.GoogleKeyInfo
import com.snurdy.backend.global.constants.ErrorCode
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.exception.ExternalApiException
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class GooglePublicKeyProvider(
    private val googleAuthClient: GoogleAuthClient
) {
    @Volatile
    private var cachedKeys: Map<String, RSAPublicKey> = emptyMap()

    fun getPublicKey(kid: String): RSAPublicKey =
        cachedKeys[kid] ?: synchronized(this) {
            cachedKeys[kid] ?: run {
                refreshKeys()
                cachedKeys[kid] ?: throw ApiException(ErrorCode.INVALID_TOKEN)
            }
        }

    private fun refreshKeys() {
        runCatching {
            googleAuthClient.getGooglePublicKeys().keys.associate { key ->
                key.kid to generatePublicKey(key)
            }
        }.onSuccess { keys ->
            cachedKeys = keys
        }.onFailure { e ->
            throw ExternalApiException(ErrorCode.GOOGLE_API_ERROR)
        }
    }

    private fun generatePublicKey(key: GoogleKeyInfo): RSAPublicKey {
        val n = Base64.getUrlDecoder().decode(key.n)
        val e = Base64.getUrlDecoder().decode(key.e)
        val spec = RSAPublicKeySpec(
            BigInteger(1, n),
            BigInteger(1, e)
        )
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }
}