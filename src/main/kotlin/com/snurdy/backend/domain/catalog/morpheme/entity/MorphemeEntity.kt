package com.snurdy.backend.domain.catalog.morpheme.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "morphemes")
class MorphemeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "spelling", nullable = false, length = 50)
    var spelling: String,

    @Column(name = "meaning", nullable = false, length = 100)
    var meaning: String
) : BaseTimeEntity()
