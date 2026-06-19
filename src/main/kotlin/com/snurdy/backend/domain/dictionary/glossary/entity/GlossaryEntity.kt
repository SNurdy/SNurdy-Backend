package com.snurdy.backend.domain.dictionary.glossary.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "glossaries")
class GlossaryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L,

    @Column(name = "category", nullable = false, length = 50)
    var category: String,

    @Column(name = "title", nullable = false, length = 100)
    var title: String
) : BaseTimeEntity()
