package com.snurdy.backend.domain.user.preference.entity

import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

// 사용자 환경 설정 (preference) 분리 테이블
// UserEntity 비대화 해소 + selectedGlossaryId 영구화 목적으로 분리
// row 는 가입 service 가 user 생성 트랜잭션 안에서 INSERT, 모든 user 에 대해 항상 존재
@Entity
@Table(name = "user_preferences")
class UserPreferenceEntity(
    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "default_test_type", length = 32)
    var defaultTestType: TestType? = null,

    @Column(name = "selected_glossary_id", nullable = false)
    var selectedGlossaryId: Long = DEFAULT_SELECTED_GLOSSARY_ID
) : BaseTimeEntity() {

    fun updateDefaultTestType(testType: TestType?) {
        this.defaultTestType = testType
    }

    fun updateSelectedGlossaryId(glossaryId: Long) {
        this.selectedGlossaryId = glossaryId
    }

    companion object {
        // 신규 유저 기본 선택 단어장 (심장내과). V6 seed data 기준 glossaryId = 6
        const val DEFAULT_SELECTED_GLOSSARY_ID: Long = 6L
    }
}
