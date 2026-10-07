package com.snurdy.backend.domain.inquiry.mail.service

import com.snurdy.backend.domain.inquiry.config.InquiryProperties
import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus
import com.snurdy.backend.domain.inquiry.mail.message.InquiryMailMessage
import com.snurdy.backend.domain.inquiry.mail.repository.InquiryMailOutboxRepository
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration

@Service
class InquiryMailPublisher(
    private val inquiryMailOutboxRepository: InquiryMailOutboxRepository,
    private val sqsTemplate: SqsTemplate,
    private val inquiryProperties: InquiryProperties,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 커밋 이후 시점과 스케줄러 모두 이어받을 트랜잭션이 없으므로 항상 새 트랜잭션으로 짧게 끊는다
    private val newTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun publishOutbox(outboxId: Long) {
        if (!changeStatus(outboxId, InquiryMailStatus.PENDING, InquiryMailStatus.PUBLISHING)) return

        try {
            sqsTemplate.send(inquiryProperties.queue.mail, InquiryMailMessage(outboxId))
        } catch (e: Exception) {
            log.warn("문의 메일 SQS 발행 실패 outboxId={}", outboxId, e)
            changeStatus(outboxId, InquiryMailStatus.PUBLISHING, InquiryMailStatus.PENDING)
            return
        }

        newTransaction.executeWithoutResult {
            inquiryMailOutboxRepository.completePublishIfMatches(
                outboxId,
                InquiryMailStatus.PUBLISHING,
                InquiryMailStatus.PUBLISHED,
                clock.instant()
            )
        }
    }

    fun recoverStalePublishingOutboxes(): Int =
        newTransaction.execute {
            val now = clock.instant()
            inquiryMailOutboxRepository.resetStaleStatus(
                InquiryMailStatus.PUBLISHING,
                InquiryMailStatus.PENDING,
                now.minus(STALE_PUBLISHING_THRESHOLD),
                now
            )
        } ?: 0

    fun findPendingOutboxIds(): List<Long> =
        inquiryMailOutboxRepository.findIdsByStatusAndCreatedBefore(
            InquiryMailStatus.PENDING,
            clock.instant().minus(PENDING_GRACE_PERIOD),
            PageRequest.of(0, REPUBLISH_BATCH_SIZE)
        )

    private fun changeStatus(outboxId: Long, expected: InquiryMailStatus, next: InquiryMailStatus): Boolean =
        newTransaction.execute {
            inquiryMailOutboxRepository.updateStatusIfMatches(outboxId, expected, next, clock.instant()) == 1
        } == true

    companion object {
        private val STALE_PUBLISHING_THRESHOLD: Duration = Duration.ofMinutes(5)
        private val PENDING_GRACE_PERIOD: Duration = Duration.ofMinutes(1)
        private const val REPUBLISH_BATCH_SIZE = 50
    }
}