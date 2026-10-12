package com.snurdy.backend.domain.inquiry.mail.event

import com.snurdy.backend.domain.inquiry.mail.service.InquiryMailPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class InquiryMailRequestedEventListener(
    private val inquiryMailPublisher: InquiryMailPublisher
) {

    // 문의 API 응답이 SQS 전송 지연에 묶이지 않도록 커밋 이후 별도 스레드에서 발행한다
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleInquiryMailRequested(event: InquiryMailRequestedEvent) {
        inquiryMailPublisher.publishOutboxToQueue(event.outboxId)
    }
}