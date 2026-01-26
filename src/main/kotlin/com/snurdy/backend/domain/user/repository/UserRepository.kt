package com.snurdy.backend.domain.user.repository

import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, UUID> {
    fun findBySocialTypeAndSocialId(socialType: SocialType, socialId: String): User?
}