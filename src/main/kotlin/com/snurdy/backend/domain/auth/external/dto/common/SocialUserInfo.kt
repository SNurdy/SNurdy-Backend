package com.snurdy.backend.domain.auth.external.dto.common

import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.global.extension.normalizeToNull

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
    name = name.normalizeToNull(),
    nickname = nickname.normalizeToNull()?.take(UserEntity.MAX_NICKNAME_LENGTH)
)
