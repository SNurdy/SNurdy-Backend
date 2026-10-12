package com.snurdy.backend.domain.inquiry.mail.service

import com.snurdy.backend.domain.inquiry.config.InquiryProperties
import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailOutboxEntity
import com.snurdy.backend.domain.inquiry.mail.repository.InquiryMailOutboxRepository
import com.snurdy.backend.domain.inquiry.mail.sender.InquiryMailSender
import com.snurdy.backend.domain.inquiry.mail.sender.toMailContent
import com.snurdy.backend.domain.inquiry.repository.InquiryRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class InquiryMailDeliveryService(
    private val inquiryMailOutboxRepository: InquiryMailOutboxRepository,
    private val inquiryRepository: InquiryRepository,
    private val inquiryMailSender: InquiryMailSender,
    private val inquiryProperties: InquiryProperties,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun deliverInquiryMail(outboxId: Long, receiveCount: Int) {
        val outbox = inquiryMailOutboxRepository.findByIdOrNull(outboxId)
        if (outbox == null) {
            log.warn("발송할 문의 메일 outbox 가 없어 메시지를 버림 outboxId={}", outboxId)
            return
        }
        if (outbox.isSent()) return

        val authorId = inquiryRepository.findByIdOrNull(outbox.inquiryId)?.userId
        val content = outbox.toMailContent(authorId, inquiryProperties.mail.subjectPrefix)

        try {
            inquiryMailSender.sendMail(content)
        } catch (e: Exception) {
            recordDeliveryFailure(outboxId, receiveCount, e)
            throw e
        }

        inquiryMailOutboxRepository.markSent(outboxId, clock.instant())
    }

    private fun recordDeliveryFailure(outboxId: Long, receiveCount: Int, e: Exception) {
        val lastError = (e.message ?: e.javaClass.simpleName).take(InquiryMailOutboxEntity.Companion.MAX_ERROR_LENGTH)

        if (receiveCount >= MAX_RECEIVE_COUNT) {
            inquiryMailOutboxRepository.markFailed(outboxId, lastError, clock.instant())
            log.error("문의 메일 발송이 {}회 실패해 DLQ 로 이동 outboxId={}", receiveCount, outboxId, e)
        } else {
            inquiryMailOutboxRepository.recordDeliveryFailure(outboxId, lastError, clock.instant())
        }
    }

    companion object {
        private const val MAX_RECEIVE_COUNT = 5
    }
}