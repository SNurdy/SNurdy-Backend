package com.snurdy.backend.domain.auth.external.dto.common

data class SocialUserInfo(
    val socialId: String,
    val email: String? = null,
    val name: String? = null,
    val nickname: String? = null
)