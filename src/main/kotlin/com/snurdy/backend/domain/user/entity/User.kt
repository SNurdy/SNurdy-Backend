package com.snurdy.backend.domain.user.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "users")
class User(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val socialId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val socialType: SocialType,

    @Column(nullable = true)
    var email: String? = null,

    @Column(nullable = false)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    @Column(nullable = false)
    var currentStreak: Int = 0,

    @Column(nullable = false)
    var longestStreak: Int = 0,

    @Column(nullable = false)
    var totalLearningTerms: Int = 0
) {
    fun updateProfile(nickname: String, email: String?) {
        this.nickname = nickname
        this.email = email
    }
}

enum class Role {
    USER, ADMIN
}