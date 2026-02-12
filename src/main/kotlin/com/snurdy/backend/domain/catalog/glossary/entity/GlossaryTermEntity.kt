package com.snurdy.backend.domain.catalog.glossary.entity

import com.snurdy.backend.domain.catalog.term.entity.TermEntity
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

@Entity
@Table(name = "glossary_terms")
class GlossaryTermEntity(
    @EmbeddedId
    val id: GlossaryTermId,

    @MapsId("glossaryId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "glossary_id", nullable = false)
    val glossary: GlossaryEntity,

    @MapsId("termId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    val term: TermEntity
) : BaseTimeEntity()
