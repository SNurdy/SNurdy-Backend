package com.snurdy.backend.domain.user.account.repository

import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findBySocialTypeAndSocialIdAndDeletedAtIsNull(socialType: SocialType, socialId: String): UserEntity?
    fun findBySocialTypeAndSocialIdAndDeletedAtIsNotNull(socialType: SocialType, socialId: String): UserEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.id = :userId")
    fun findByIdForUpdate(
        @Param("userId") userId: UUID
    ): UserEntity?
}