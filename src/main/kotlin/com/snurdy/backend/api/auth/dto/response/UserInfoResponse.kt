package com.snurdy.backend.api.auth.dto.response

import com.snurdy.backend.domain.user.entity.SocialType

data class UserInfoResponse(
    val userId: Long,
    val email: String,
    val nickname: String?,
    val profileImageUrl: String?,
    val provider: SocialType
)