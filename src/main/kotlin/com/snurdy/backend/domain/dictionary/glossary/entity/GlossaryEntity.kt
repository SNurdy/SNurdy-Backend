package com.snurdy.backend.domain.dictionary.glossary.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "glossaries",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_glossary_share_code", columnNames = ["share_code"])
    ]
)
class GlossaryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L,

    @Column(name = "category", nullable = false, length = 50)
    var category: String,

    @Column(name = "title", nullable = false, length = 100)
    var title: String,

    // 단어장 종류 구분
    // DEFAULT 외에는 ownerId 가 반드시 채워진다
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    val type: GlossaryType = GlossaryType.DEFAULT,

    // CUSTOM/WRONG_ANSWER 의 소유 사용자
    // DEFAULT 면 null
    @Column(name = "owner_id", columnDefinition = "BINARY(16)")
    val ownerId: UUID? = null,

    // 공유 코드
    // null 이면 공유 비활성 상태
    // 활성화 시 12자 unique 코드 부여
    @Column(name = "share_code", length = 12)
    var shareCode: String? = null
) : BaseTimeEntity()
