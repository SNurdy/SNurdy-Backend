package com.snurdy.backend.domain.learning.entity

import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.catalog.term.entity.TermEntity
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "term_progresses",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_term_progress_user_glossary_term",
            columnNames = ["user_id", "glossary_id", "term_id"]
        )
    ],
    indexes = [
        Index(name = "idx_term_progress_next_review", columnList = "user_id,next_review_at")
    ]
)
class TermProgressEntity(
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "glossary_id", nullable = false)
    val glossary: GlossaryEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    val term: TermEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: TermProgressStatus = TermProgressStatus.NEW,

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    var easeFactor: BigDecimal = DEFAULT_EASE_FACTOR,

    @Column(name = "interval_days", nullable = false)
    var intervalDays: Int = 0,

    @Column(name = "review_count", nullable = false)
    var reviewCount: Int = 0,

    @Column(name = "last_reviewed_at")
    var lastReviewedAt: Instant? = null,

    @Column(name = "next_review_at")
    var nextReviewAt: Instant? = null
) : BaseTimeEntity() {

    fun markLearned(now: Instant, nextReviewAt: Instant) {
        status = TermProgressStatus.LEARNING
        reviewCount += 1
        lastReviewedAt = now
        this.nextReviewAt = nextReviewAt
    }

    fun markMastered(now: Instant) {
        status = TermProgressStatus.MASTERED
        lastReviewedAt = now
        nextReviewAt = null
    }

    companion object {
        private val DEFAULT_EASE_FACTOR: BigDecimal = BigDecimal("2.50")
    }
}