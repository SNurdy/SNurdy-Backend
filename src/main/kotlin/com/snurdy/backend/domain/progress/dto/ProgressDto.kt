package com.snurdy.backend.domain.progress.dto

// 진도 갱신 이벤트 1건. TestService 등 호출자가 ProgressUpdateService 에 위임할 때 사용
data class ProgressEvent(
    val glossaryId: Long,
    val termId: Long,
    val correct: Boolean
)
