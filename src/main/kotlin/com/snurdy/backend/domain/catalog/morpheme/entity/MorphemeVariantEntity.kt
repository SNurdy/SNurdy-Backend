package com.snurdy.backend.domain.catalog.morpheme.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "morpheme_variants",
    indexes = [
        Index(name = "idx_mv_variant", columnList = "variant"),
        Index(name = "idx_mv_morpheme", columnList = "morpheme_id")
    ]
)
class MorphemeVariantEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "morpheme_id", nullable = false)
    val morpheme: MorphemeEntity,

    @Column(nullable = false, length = 50)
    val variant: String,

    @Column(nullable = false, length = 50)
    val base: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
