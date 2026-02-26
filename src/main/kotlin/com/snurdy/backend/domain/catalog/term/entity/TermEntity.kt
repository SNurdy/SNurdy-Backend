package com.snurdy.backend.domain.catalog.term.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "terms")
class TermEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "spelling", nullable = false, length = 100)
    var spelling: String,

    @Column(name = "meaning", nullable = false, length = 255)
    var meaning: String,

    @Column(name = "abbreviation", length = 50)
    var abbreviation: String? = null,

    @Column(name = "explanation", columnDefinition = "TEXT")
    var explanation: String? = null,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
) : BaseTimeEntity()