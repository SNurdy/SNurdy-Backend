package com.snurdy.backend.api.auth.dto.request

data class KakaoLoginRequest(
    val authorizationCode: String
)