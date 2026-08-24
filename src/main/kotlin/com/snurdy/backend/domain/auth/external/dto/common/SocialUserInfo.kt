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

// nickname 은 랜덤 생성 폴백이 필요한 외부 계산 결과라 호출자가 확정해 넘긴다
fun SocialUserInfo.toEntity(socialType: SocialType, nickname: String) = UserEntity(
    socialId = socialId,
    socialType = socialType,
    email = email,
    name = name.normalizeToNull(),
    nickname = nickname.take(UserEntity.MAX_NICKNAME_LENGTH)
)
