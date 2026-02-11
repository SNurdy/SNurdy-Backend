package com.snurdy.backend.domain.learning.entity

import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryEntity
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
import java.time.Instant

@Entity
@Table(name = "glossary_progresses")
class GlossaryProgressEntity(
    @EmbeddedId
    val id: GlossaryProgressId,

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @MapsId("glossaryId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "glossary_id", nullable = false)
    val glossary: GlossaryEntity,

    @Column(name = "learned_count", nullable = false)
    var learnedCount: Int = 0,

    @Column(name = "last_learned_at")
    var lastLearnedAt: Instant? = null
) : BaseTimeEntity()
