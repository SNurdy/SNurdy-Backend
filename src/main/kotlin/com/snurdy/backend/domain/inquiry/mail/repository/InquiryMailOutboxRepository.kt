package com.snurdy.backend.domain.inquiry.mail.repository

import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailOutboxEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// 상태 조건과 변경을 한 문장에서 처리해야 여러 인스턴스가 동시에 호출해도 한쪽만 성공한다
// 전이 쿼리의 반환값이 1 이면 성공이고 0 이면 다른 스레드나 인스턴스가 먼저 상태를 바꾼 것이다
interface InquiryMailOutboxRepository : JpaRepository<InquiryMailOutboxEntity, Long> {

    // PENDING 에서 PUBLISHING 으로 선점
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PUBLISHING,
               o.updatedAt = :now
         WHERE o.id = :outboxId
           AND o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PENDING
        """
    )
    fun claimForPublishing(@Param("outboxId") outboxId: Long, @Param("now") now: Instant): Int

    // PUBLISHING 에서 PUBLISHED 로 완료
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PUBLISHED,
               o.publishedAt = :now,
               o.updatedAt = :now
         WHERE o.id = :outboxId
           AND o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PUBLISHING
        """
    )
    fun markPublished(@Param("outboxId") outboxId: Long, @Param("now") now: Instant): Int

    // 전송 실패로 PUBLISHING 에서 PENDING 으로 반납
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PENDING,
               o.updatedAt = :now
         WHERE o.id = :outboxId
           AND o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PUBLISHING
        """
    )
    fun releaseToPending(@Param("outboxId") outboxId: Long, @Param("now") now: Instant): Int

    // 선점한 서버가 중단되어 PUBLISHING 에 남은 행을 한꺼번에 PENDING 으로 반납
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PENDING,
               o.updatedAt = :now
         WHERE o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PUBLISHING
           AND o.updatedAt < :abandonedBefore
        """
    )
    fun releaseAbandonedPublishing(
        @Param("abandonedBefore") abandonedBefore: Instant,
        @Param("now") now: Instant
    ): Int

    @Query(
        """
        SELECT o.id
          FROM InquiryMailOutboxEntity o
         WHERE o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.PENDING
           AND o.createdAt < :createdBefore
         ORDER BY o.id
        """
    )
    fun findPendingIdsCreatedBefore(
        @Param("createdBefore") createdBefore: Instant,
        pageable: Pageable
    ): List<Long>

    // 중복 메시지가 동시에 처리되어도 먼저 기록된 SENT 를 덮어쓰지 않도록 SENT 가 아닐 때만 기록
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.SENT,
               o.attemptCount = o.attemptCount + 1,
               o.sentAt = :now,
               o.updatedAt = :now
         WHERE o.id = :outboxId
           AND o.status <> com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.SENT
        """
    )
    fun markSent(@Param("outboxId") outboxId: Long, @Param("now") now: Instant): Int

    // 재시도 전 실패 횟수와 원인만 남기고 상태는 유지
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.attemptCount = o.attemptCount + 1,
               o.lastError = :lastError,
               o.updatedAt = :now
         WHERE o.id = :outboxId
           AND o.status <> com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.SENT
        """
    )
    fun recordDeliveryFailure(
        @Param("outboxId") outboxId: Long,
        @Param("lastError") lastError: String,
        @Param("now") now: Instant
    ): Int

    // 마지막 시도까지 실패해 DLQ 로 넘어가는 행을 FAILED 로 기록
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.FAILED,
               o.attemptCount = o.attemptCount + 1,
               o.lastError = :lastError,
               o.updatedAt = :now
         WHERE o.id = :outboxId
           AND o.status <> com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus.SENT
        """
    )
    fun markFailed(
        @Param("outboxId") outboxId: Long,
        @Param("lastError") lastError: String,
        @Param("now") now: Instant
    ): Int
}