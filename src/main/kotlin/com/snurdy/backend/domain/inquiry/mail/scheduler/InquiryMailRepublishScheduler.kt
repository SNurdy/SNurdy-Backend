package com.snurdy.backend.domain.inquiry.mail.scheduler

import com.snurdy.backend.domain.inquiry.mail.service.InquiryMailPublisher
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class InquiryMailRepublishScheduler(
    private val inquiryMailPublisher: InquiryMailPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun republishPendingMails() {
        val recoveredCount = inquiryMailPublisher.recoverStalePublishingOutboxes()
        if (recoveredCount > 0) {
            log.warn("PUBLISHING 상태로 멈춘 문의 메일 {}건을 재발행 대상으로 되돌림", recoveredCount)
        }

        inquiryMailPublisher.findRepublishableOutboxIds().forEach(inquiryMailPublisher::publishOutboxToQueue)
    }
}