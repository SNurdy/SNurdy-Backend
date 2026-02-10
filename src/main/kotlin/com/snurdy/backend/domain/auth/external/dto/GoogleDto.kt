package com.snurdy.backend.domain.auth.external.dto

data class GoogleKeysResponse(
    val keys: List<GoogleKeyInfo>
)

data class GoogleKeyInfo(
    val kty: String,
    val kid: String,
    val use: String,
    val alg: String,
    val n: String,
    val e: String
)