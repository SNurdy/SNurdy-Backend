package com.snurdy.backend.domain.termMemo.entity

import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

@Entity
@Table(name = "user_term_memos")
class UserTermMemoEntity(
    @EmbeddedId
    val id: UserTermMemoId,

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @MapsId("termId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    val term: TermEntity,

    // 단어 카드에 사용자가 남기는 개인 메모
    // 500자 제한은 DB 컬럼과 동일하게 유지
    @Column(name = "content", nullable = false, length = MAX_CONTENT_LENGTH)
    var content: String
) : BaseTimeEntity() {

    fun updateContent(newContent: String) {
        this.content = newContent.take(MAX_CONTENT_LENGTH)
    }

    companion object {
        const val MAX_CONTENT_LENGTH = 500
    }
}
