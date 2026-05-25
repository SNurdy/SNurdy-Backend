package com.snurdy.backend.domain.user.wrongAnswerNote.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "wrong_answer_terms")
class WrongAnswerTermEntity(
    @EmbeddedId
    val id: WrongAnswerTermId,

    @Column(name = "source_glossary_id")
    var sourceGlossaryId: Long? = null,

    @Column(name = "incorrect_at", nullable = false)
    var incorrectAt: Instant,

    @Column(name = "total_incorrect_count", nullable = false)
    var totalIncorrectCount: Int = 0,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)
