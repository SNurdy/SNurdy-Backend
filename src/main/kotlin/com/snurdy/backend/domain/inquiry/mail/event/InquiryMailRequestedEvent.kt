package com.snurdy.backend.domain.inquiry.mail.event

data class InquiryMailRequestedEvent(
    val outboxId: Long
)