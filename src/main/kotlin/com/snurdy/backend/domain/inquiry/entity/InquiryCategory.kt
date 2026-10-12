package com.snurdy.backend.domain.inquiry.entity

// 기획 확정 전 더미 유형이며 label 은 개발팀 메일 표시용
enum class InquiryCategory(
    val label: String
) {
    BUG("버그 신고"),
    CONTENT_ERROR("단어/뜻 오류 제보"),
    FEATURE_REQUEST("기능 제안"),
    ACCOUNT("계정/로그인"),
    ETC("기타")
}