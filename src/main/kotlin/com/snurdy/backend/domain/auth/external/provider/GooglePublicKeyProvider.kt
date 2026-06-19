package com.snurdy.backend.domain.auth.external.provider

import com.snurdy.backend.domain.auth.external.client.GoogleAuthClient
import com.snurdy.backend.global.constants.AuthError
import org.springframework.stereotype.Component
import java.security.interfaces.RSAPublicKey

@Component
class GooglePublicKeyProvider(
    private val googleAuthClient: GoogleAuthClient
) : RsaPublicKeyProvider(
    fetchError = AuthError.GOOGLE_API_ERROR,
    keyNotFoundError = AuthError.INVALID_TOKEN
) {
    override fun fetchKeys(): Map<String, RSAPublicKey> =
        googleAuthClient.getGooglePublicKeys().keys.associate { key ->
            key.kid to generatePublicKey(key.n, key.e)
        }
}
