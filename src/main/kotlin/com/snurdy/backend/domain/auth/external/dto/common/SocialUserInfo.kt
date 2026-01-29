package com.snurdy.backend.domain.auth.external.dto.common

data class SocialUserInfo(
    val socialId: String,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String? = null
)