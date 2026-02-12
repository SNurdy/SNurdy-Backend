package com.snurdy.backend.domain.bookmark.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class BookmarkId(
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    val userId: UUID,

    @Column(name = "glossary_id", nullable = false)
    val glossaryId: Long,

    @Column(name = "term_id", nullable = false)
    val termId: Long
) : Serializable
