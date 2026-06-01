package com.snurdy.backend.domain.user.customNote.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class CustomNoteTermId(
    @Column(name = "note_id", nullable = false)
    val noteId: Long = 0L,

    @Column(name = "term_id", nullable = false)
    val termId: Long = 0L
) : Serializable
