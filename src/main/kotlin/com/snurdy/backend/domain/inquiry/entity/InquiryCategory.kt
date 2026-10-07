package com.snurdy.backend.domain.inquiry.entity

// 기획 확정 전 더미 유형
// 유형을 없앨 때는 enum 을 지우지 말고 selectable 만 false 로 바꿔 기존 데이터와 호환 유지
enum class InquiryCategory(
    val label: String,
    val selectable: Boolean
) {
    BUG("버그 신고", true),
    CONTENT_ERROR("단어/뜻 오류 제보", true),
    FEATURE_REQUEST("기능 제안", true),
    ACCOUNT("계정/로그인", true),
    ETC("기타", true)
}