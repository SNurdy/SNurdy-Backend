package com.snurdy.backend.domain.learning.dto

// 학습 결과 1건. TestService 가 LearningCommandService 에 위임할 때 사용한다.
data class LearningAnswerEvent(
    val glossaryId: Long,
    val termId: Long,
    val correct: Boolean
)
