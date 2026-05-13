package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.global.constants.ErrorCode
import com.snurdy.backend.global.exception.ApiException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

abstract class RsaPublicKeyProvider(
    private val fetchError: ErrorCode,
    private val keyNotFoundError: ErrorCode
) {
    @Volatile
    private var cachedKeys: Map<String, RSAPublicKey> = emptyMap()

    fun getPublicKey(kid: String): RSAPublicKey {
        cachedKeys[kid]?.let { return it }
        return synchronized(this) {
            cachedKeys[kid] ?: run {
                refreshKeys()
                cachedKeys[kid] ?: throw ApiException(keyNotFoundError)
            }
        }
    }

    private fun refreshKeys() {
        runCatching { fetchKeys() }
            .onSuccess { cachedKeys = it }
            .onFailure { throw ApiException(fetchError) }
    }

    protected abstract fun fetchKeys(): Map<String, RSAPublicKey>

    protected fun generatePublicKey(n: String, e: String): RSAPublicKey {
        val nBytes = Base64.getUrlDecoder().decode(n)
        val eBytes = Base64.getUrlDecoder().decode(e)
        val spec = RSAPublicKeySpec(BigInteger(1, nBytes), BigInteger(1, eBytes))
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }
}
