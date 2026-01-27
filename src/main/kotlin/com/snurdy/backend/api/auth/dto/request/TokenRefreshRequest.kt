package com.snurdy.backend.api.auth.dto.request

data class TokenRefreshRequest(
    val refreshToken: String
)