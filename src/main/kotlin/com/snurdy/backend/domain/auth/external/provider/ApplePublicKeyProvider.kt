package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.client.AppleAuthClient
import com.snurdy.backend.domain.auth.external.dto.AppleKeyInfo
import com.snurdy.backend.global.constants.ErrorCode
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class ApplePublicKeyProvider(
    private val appleAuthClient: AppleAuthClient
) {
    @Volatile
    private var cachedKeys: Map<String, RSAPublicKey> = emptyMap()

    fun getPublicKey(kid: String): RSAPublicKey {
        cachedKeys[kid]?.let { return it }

        return synchronized(this) {
            cachedKeys[kid] ?: run {
                refreshKeys()
                cachedKeys[kid] ?: throw ApiException(ErrorCode.APPLE_API_ERROR)
            }
        }
    }

    private fun refreshKeys() {
        runCatching {
            appleAuthClient.getApplePublicKeys()
        }.onSuccess { response ->
            cachedKeys = response.keys.associate { key ->
                key.kid to generatePublicKey(key)
            }
        }.onFailure {
            throw ApiException(ErrorCode.APPLE_API_ERROR)
        }
    }

    private fun generatePublicKey(key: AppleKeyInfo): RSAPublicKey {
        val n = Base64.getUrlDecoder().decode(key.n)
        val e = Base64.getUrlDecoder().decode(key.e)
        val spec = RSAPublicKeySpec(
            BigInteger(1, n),
            BigInteger(1, e)
        )
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }
}