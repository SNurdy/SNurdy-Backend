package com.snurdy.backend.domain.dictionary.glossary.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class GlossaryTermId(
    @Column(name = "glossary_id", nullable = false)
    val glossaryId: Long,

    @Column(name = "term_id", nullable = false)
    val termId: Long
) : Serializable
