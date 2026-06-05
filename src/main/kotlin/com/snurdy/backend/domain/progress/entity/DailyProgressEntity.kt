package com.snurdy.backend.domain.progress.entity

import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

@Entity
@Table(name = "daily_progresses")
class DailyProgressEntity(
    @EmbeddedId
    val id: DailyProgressId,

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(name = "learned_count", nullable = false)
    var learnedCount: Int = 0,

    @Column(name = "completed_count", nullable = false)
    var completedCount: Int = 0
) : BaseTimeEntity()
