package com.snurdy.backend.domain.inquiry.mail.service

import com.snurdy.backend.domain.inquiry.config.InquiryProperties
import com.snurdy.backend.domain.inquiry.mail.message.InquiryMailMessage
import com.snurdy.backend.domain.inquiry.mail.repository.InquiryMailOutboxRepository
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class InquiryMailPublisher(
    private val inquiryMailOutboxRepository: InquiryMailOutboxRepository,
    private val sqsTemplate: SqsTemplate,
    private val inquiryProperties: InquiryProperties,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishOutboxToQueue(outboxId: Long) {
        val claimed = inquiryMailOutboxRepository.claimForPublishing(outboxId, clock.instant()) == 1
        if (!claimed) return

        try {
            sqsTemplate.send(inquiryProperties.mail.queueName, InquiryMailMessage(outboxId))
        } catch (e: Exception) {
            log.warn("문의 메일 SQS 발행 실패로 PENDING 으로 되돌림 outboxId={}", outboxId, e)
            inquiryMailOutboxRepository.releaseToPending(outboxId, clock.instant())
            return
        }

        inquiryMailOutboxRepository.markPublished(outboxId, clock.instant())
    }
}