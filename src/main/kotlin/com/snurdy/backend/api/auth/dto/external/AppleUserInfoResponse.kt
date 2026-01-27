package com.snurdy.backend.api.auth.dto.external

data class AppleUserInfoResponse(
    val sub: String,
    val email: String?,
    val emailVerified: String?,
    val isPrivateEmail: String?,
    val realUserStatus: Int?
)