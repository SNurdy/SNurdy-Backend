package com.snurdy.backend.domain.dictionary.glossary.entity

enum class GlossaryType {
    // 관리자가 등록한 기본 단어장 (의료 분야별 고정 자료)
    DEFAULT,

    // 사용자가 만든 커스텀 단어장
    CUSTOM,

    // 학습 결과 자동 생성되는 오답 단어장
    WRONG_ANSWER
}
