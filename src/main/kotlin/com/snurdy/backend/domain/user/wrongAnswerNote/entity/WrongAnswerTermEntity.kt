package com.snurdy.backend.domain.user.wrongAnswerNote.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

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
) {

    // 재오답 발생. soft delete 상태였다면 자동 복구하면서 누적값은 보존
    fun recordIncorrect(sourceGlossaryId: Long, occurredAt: Instant) {
        this.sourceGlossaryId = sourceGlossaryId
        this.incorrectAt = occurredAt
        this.totalIncorrectCount += 1
        this.deletedAt = null
    }

    fun softDelete(at: Instant) {
        this.deletedAt = at
    }

    fun isActive(): Boolean = deletedAt == null

    companion object {
        // 처음 오답 발생 시 신규 row 생성
        fun firstIncorrect(
            userId: UUID,
            termId: Long,
            sourceGlossaryId: Long,
            occurredAt: Instant
        ): WrongAnswerTermEntity = WrongAnswerTermEntity(
            id = WrongAnswerTermId(userId = userId, termId = termId),
            sourceGlossaryId = sourceGlossaryId,
            incorrectAt = occurredAt,
            totalIncorrectCount = 1,
            deletedAt = null
        )
    }
}
