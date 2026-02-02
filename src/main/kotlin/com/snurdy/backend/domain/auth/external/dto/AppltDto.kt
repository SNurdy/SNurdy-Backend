package com.snurdy.backend.domain.auth.external.dto

data class AppleKeysResponse(val keys: List<AppleKeyInfo>)

data class AppleKeyInfo(
    val kty: String,
    val kid: String,
    val use: String,
    val alg: String,
    val n: String,
    val e: String
)