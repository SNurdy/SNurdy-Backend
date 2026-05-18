package com.snurdy.backend.domain.dictionary.morpheme.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.*

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
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "morpheme_id", nullable = false)
    val morpheme: MorphemeEntity,

    @Column(nullable = false, length = 50)
    val variant: String,

    @Column(nullable = false, length = 50)
    val base: String
) : BaseTimeEntity()
