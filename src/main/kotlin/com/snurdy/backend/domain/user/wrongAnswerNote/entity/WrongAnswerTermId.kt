package com.snurdy.backend.domain.user.wrongAnswerNote.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class WrongAnswerTermId(
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "term_id")
    val termId: Long = 0L
) : Serializable
