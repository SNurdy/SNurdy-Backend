package com.snurdy.backend.domain.inquiry.mail.consumer

import com.snurdy.backend.domain.inquiry.mail.message.InquiryMailMessage
import com.snurdy.backend.domain.inquiry.mail.service.InquiryMailDeliveryService
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.SqsHeaders
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

// SQS 가 없는 테스트 환경에서 리스너가 큐를 찾지 못해 부팅이 실패하지 않도록 끌 수 있게 한다
@Component
@ConditionalOnProperty(name = ["inquiry.mail.consumer-enabled"], havingValue = "true", matchIfMissing = true)
class InquiryMailConsumer(
    private val inquiryMailDeliveryService: InquiryMailDeliveryService
) {

    @SqsListener("\${inquiry.mail.queue-name}")
    fun consumeInquiryMail(
        message: InquiryMailMessage,
        @Header(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT) receiveCount: String
    ) {
        inquiryMailDeliveryService.deliverInquiryMail(message.outboxId, receiveCount.toInt())
    }
}