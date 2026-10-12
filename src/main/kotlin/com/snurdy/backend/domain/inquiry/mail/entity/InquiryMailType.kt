package com.snurdy.backend.domain.inquiry.mail.entity

enum class InquiryMailType(
    val label: String
) {
    CREATED("신규"),
    UPDATED("수정됨")
}