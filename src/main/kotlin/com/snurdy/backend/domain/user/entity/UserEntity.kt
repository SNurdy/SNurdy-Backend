package com.snurdy.backend.domain.user.entity

import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_user_social_id_type",
        columnNames = ["socialId", "socialType"]
    )]
)
class UserEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val socialId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val socialType: SocialType,

    @Column(nullable = true)
    var email: String? = null,

    @Column(nullable = false, length = 20)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.USER,

    @Column(nullable = false)
    var currentStreak: Int = 0,

    @Column(nullable = false)
    var totalLearningTerms: Int = 0,

    @Column(nullable = false)
    var longestStreak: Int = 0
) {
    fun recordLearning() {
        this.currentStreak++
        this.totalLearningTerms++
        if (this.currentStreak > this.longestStreak) {
            this.longestStreak = this.currentStreak
        }
    }

    companion object {
        fun from(info: SocialUserInfo, type: SocialType): UserEntity {
            val rawNickname = info.nickname ?: "SNurdy_${UUID.randomUUID().toString().substring(0, 5)}"
            val truncatedNickname = if (rawNickname.length > 20) rawNickname.take(20) else rawNickname

            return UserEntity(
                socialId = info.socialId,
                socialType = type,
                email = info.email,
                nickname = truncatedNickname
            )
        }
    }
}