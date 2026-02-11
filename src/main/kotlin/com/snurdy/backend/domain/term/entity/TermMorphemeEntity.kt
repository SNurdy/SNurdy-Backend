package com.snurdy.backend.domain.term.entity

import com.snurdy.backend.domain.morpheme.entity.MorphemeEntity
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

@Entity
@Table(name = "term_morphemes")
class TermMorphemeEntity(
    @EmbeddedId
    val id: TermMorphemeId,

    @MapsId("termId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    val term: TermEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "morpheme_id", nullable = false)
    val morpheme: MorphemeEntity
)
