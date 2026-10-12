package com.snurdy.backend.domain.inquiry.mail.scheduler

import com.snurdy.backend.domain.inquiry.mail.repository.InquiryMailOutboxRepository
import com.snurdy.backend.domain.inquiry.mail.service.InquiryMailPublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class InquiryMailRepublishScheduler(
    private val inquiryMailOutboxRepository: InquiryMailOutboxRepository,
    private val inquiryMailPublisher: InquiryMailPublisher,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun republishPendingMails() {
        val now = clock.instant()

        val releasedCount = inquiryMailOutboxRepository.releaseAbandonedPublishing(
            abandonedBefore = now.minus(ABANDONED_PUBLISHING_TIMEOUT),
            now = now
        )
        if (releasedCount > 0) {
            log.warn("선점한 서버가 중단되어 PUBLISHING 에 남아 있던 문의 메일 {}건을 PENDING 으로 되돌림", releasedCount)
        }

        inquiryMailOutboxRepository
            .findPendingIdsCreatedBefore(
                createdBefore = now.minus(IMMEDIATE_PUBLISH_GRACE_PERIOD),
                pageable = PageRequest.of(0, REPUBLISH_BATCH_SIZE)
            )
            .forEach(inquiryMailPublisher::publishOutboxToQueue)
    }

    companion object {
        private val ABANDONED_PUBLISHING_TIMEOUT: Duration = Duration.ofMinutes(5)
        private val IMMEDIATE_PUBLISH_GRACE_PERIOD: Duration = Duration.ofMinutes(1)
        private const val REPUBLISH_BATCH_SIZE = 50
    }
}