package com.snurdy.backend.domain.learning.entity

import com.snurdy.backend.domain.user.entity.UserEntity
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
@Table(name = "daily_records")
class DailyLearningEntity(
    @EmbeddedId
    val id: DailyLearningId,

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(name = "learned_count", nullable = false)
    var learnedCount: Int = 0,

    @Column(name = "reviewed_count", nullable = false)
    var reviewedCount: Int = 0
) : BaseTimeEntity()
