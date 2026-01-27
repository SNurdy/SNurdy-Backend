package com.snurdy.backend.api.auth.dto.request

data class AppleLoginRequest(
    val authorizationCode: String,
    val idToken: String
)