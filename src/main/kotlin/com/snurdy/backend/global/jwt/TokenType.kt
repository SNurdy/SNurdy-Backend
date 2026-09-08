package com.snurdy.backend.global.jwt

enum class TokenType(val claimValue: String) {
    ACCESS("ACCESS"),
    REFRESH("REFRESH");

    companion object {
        private val BY_CLAIM = entries.associateBy { it.claimValue.uppercase() }

        fun fromClaim(value: String?): TokenType? =
            value?.uppercase()?.let(BY_CLAIM::get)
    }
}
