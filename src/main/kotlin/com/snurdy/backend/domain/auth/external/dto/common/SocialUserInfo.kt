package com.snurdy.backend.domain.auth.external.dto.common

import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity

data class SocialUserInfo(
    val socialId: String,
    val email: String? = null,
    val name: String? = null,
    val nickname: String? = null
)

fun SocialUserInfo.toEntity(socialType: SocialType) = UserEntity(
    socialId = socialId,
    socialType = socialType,
    email = email,
    name = name,
    nickname = nickname?.take(UserEntity.MAX_NICKNAME_LENGTH)
)
