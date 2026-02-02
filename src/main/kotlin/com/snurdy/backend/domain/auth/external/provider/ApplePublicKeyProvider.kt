package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.global.common.constants.ErrorCode
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class ApplePublicKeyProvider {

    private val appleKeysUrl = "https://appleid.apple.com/auth/keys"
    private val restTemplate = RestTemplate()

    @Volatile
    private var cachedKeys: Map<String, RSAPublicKey> = emptyMap()

    fun getPublicKey(kid: String): RSAPublicKey {
        cachedKeys[kid]?.let { return it }

        refreshKeys()

        return cachedKeys[kid]
            ?: throw ApiException(ErrorCode.APPLE_API_ERROR)
    }

    private fun refreshKeys() {
        val response = restTemplate.getForObject(appleKeysUrl, AppleKeysResponse::class.java)
            ?: throw ApiException(ErrorCode.APPLE_API_ERROR)

        cachedKeys = response.keys.associate { key ->
            key.kid to generatePublicKey(key)
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

data class AppleKeysResponse(val keys: List<AppleKeyInfo>)

data class AppleKeyInfo(
    val kty: String,
    val kid: String,
    val use: String,
    val alg: String,
    val n: String,
    val e: String
)