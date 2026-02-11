package com.snurdy.backend.domain.user.entity

import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_social_id_type",
            columnNames = ["social_id", "social_type"]
        )
    ]
)
class UserEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "social_id", nullable = false)
    val socialId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    val socialType: SocialType,

    @Column(nullable = true)
    var email: String? = null,

    @Column(nullable = true, length = 50)
    var name: String? = null,

    @Column(nullable = false, length = 20)
    var nickname: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: Role = Role.USER,

    @Column(nullable = false)
    var currentStreak: Int = 0,

    @Column(nullable = false)
    var totalLearningTerms: Int = 0,

    @Column(nullable = false)
    var longestStreak: Int = 0
) : BaseTimeEntity() {

    companion object {
        private const val MAX_NICKNAME_LENGTH = 20

        fun from(info: SocialUserInfo, type: SocialType) = UserEntity(
            socialId = info.socialId,
            socialType = type,
            email = info.email,
            name = info.name,
            nickname = info.nickname?.take(MAX_NICKNAME_LENGTH) ?: ""
        )
    }
}