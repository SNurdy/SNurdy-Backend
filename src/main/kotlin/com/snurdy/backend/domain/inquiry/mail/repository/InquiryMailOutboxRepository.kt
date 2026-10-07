package com.snurdy.backend.domain.inquiry.mail.repository

import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailOutboxEntity
import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface InquiryMailOutboxRepository : JpaRepository<InquiryMailOutboxEntity, Long> {

    // 영향 행이 0 이면 다른 스레드나 블루그린 반대편 인스턴스가 먼저 선점한 것
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = :nextStatus,
               o.updatedAt = :now
         WHERE o.id = :outboxId
           AND o.status = :expectedStatus
        """
    )
    fun updateStatusIfMatches(
        @Param("outboxId") outboxId: Long,
        @Param("expectedStatus") expectedStatus: InquiryMailStatus,
        @Param("nextStatus") nextStatus: InquiryMailStatus,
        @Param("now") now: Instant
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = :nextStatus,
               o.publishedAt = :publishedAt,
               o.updatedAt = :publishedAt
         WHERE o.id = :outboxId
           AND o.status = :expectedStatus
        """
    )
    fun completePublishIfMatches(
        @Param("outboxId") outboxId: Long,
        @Param("expectedStatus") expectedStatus: InquiryMailStatus,
        @Param("nextStatus") nextStatus: InquiryMailStatus,
        @Param("publishedAt") publishedAt: Instant
    ): Int

    // 선점 직후 서버가 종료되어 멈춘 row 를 재발행 대상으로 되돌림
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE InquiryMailOutboxEntity o
           SET o.status = :resetStatus,
               o.updatedAt = :now
         WHERE o.status = :staleStatus
           AND o.updatedAt < :updatedBefore
        """
    )
    fun resetStaleStatus(
        @Param("staleStatus") staleStatus: InquiryMailStatus,
        @Param("resetStatus") resetStatus: InquiryMailStatus,
        @Param("updatedBefore") updatedBefore: Instant,
        @Param("now") now: Instant
    ): Int

    @Query(
        """
        SELECT o.id
          FROM InquiryMailOutboxEntity o
         WHERE o.status = :status
           AND o.createdAt < :createdBefore
         ORDER BY o.id
        """
    )
    fun findIdsByStatusAndCreatedBefore(
        @Param("status") status: InquiryMailStatus,
        @Param("createdBefore") createdBefore: Instant,
        pageable: Pageable
    ): List<Long>
}