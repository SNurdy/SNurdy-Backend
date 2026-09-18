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

    @Column(name = "glossary_id")
    var glossaryId: Long? = null,

    @Column(name = "incorrect_at", nullable = false)
    var incorrectAt: Instant,

    @Column(name = "total_incorrect_count", nullable = false)
    var totalIncorrectCount: Int = 0,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
) {

    // 재오답 발생. soft delete 상태였다면 자동 복구하면서 누적값은 보존
    fun recordIncorrect(glossaryId: Long, occurredAt: Instant) {
        this.glossaryId = glossaryId
        this.incorrectAt = occurredAt
        this.totalIncorrectCount += 1
        this.deletedAt = null
    }

    fun softDelete(at: Instant) {
        this.deletedAt = at
    }

    fun isActive(): Boolean = deletedAt == null
}
