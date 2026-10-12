package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.client.AppleAuthClient
import com.snurdy.backend.global.constants.AuthError
import org.springframework.stereotype.Component
import java.security.interfaces.RSAPublicKey

@Component
class ApplePublicKeyProvider(
    private val appleAuthClient: AppleAuthClient
) : RsaPublicKeyProvider(
    fetchError = AuthError.APPLE_API_ERROR,
    keyNotFoundError = AuthError.INVALID_TOKEN
) {
    override fun fetchKeys(): Map<String, RSAPublicKey> =
        appleAuthClient.getApplePublicKeys().keys.associate { key ->
            key.kid to generatePublicKey(key.n, key.e)
        }
}
