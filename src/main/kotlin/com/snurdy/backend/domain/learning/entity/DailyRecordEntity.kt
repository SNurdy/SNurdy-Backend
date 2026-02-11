package com.snurdy.backend.domain.learning.entity

import com.snurdy.backend.domain.user.entity.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "date_infos")
class DailyRecordEntity(
    @EmbeddedId
    val id: DailyRecordId,

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(name = "study_count", nullable = false)
    var studyCount: Int = 0,

    @Column(name = "review_count", nullable = false)
    var reviewCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
