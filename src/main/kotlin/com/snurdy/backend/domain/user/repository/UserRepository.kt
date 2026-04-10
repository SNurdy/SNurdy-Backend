package com.snurdy.backend.domain.user.repository

import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findBySocialTypeAndSocialIdAndDeletedAtIsNull(socialType: SocialType, socialId: String): UserEntity?
    fun findBySocialTypeAndSocialIdAndDeletedAtIsNotNull(socialType: SocialType, socialId: String): UserEntity?
    fun findBySocialTypeAndSocialId(socialType: SocialType, socialId: String): UserEntity?
}