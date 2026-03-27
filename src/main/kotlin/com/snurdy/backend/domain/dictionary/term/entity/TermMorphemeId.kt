package com.snurdy.backend.domain.dictionary.term.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class TermMorphemeId(
    @Column(name = "term_id", nullable = false)
    val termId: Long,

    @Column(name = "position", nullable = false)
    val position: Int
) : Serializable
