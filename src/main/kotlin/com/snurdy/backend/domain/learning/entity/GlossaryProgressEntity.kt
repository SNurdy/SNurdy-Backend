package com.snurdy.backend.domain.learning.entity

import com.snurdy.backend.domain.glossary.entity.GlossaryEntity
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

    @Column(name = "studied_count", nullable = false)
    var studiedCount: Int = 0,

    @Column(name = "last_studied_at")
    var lastStudiedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
