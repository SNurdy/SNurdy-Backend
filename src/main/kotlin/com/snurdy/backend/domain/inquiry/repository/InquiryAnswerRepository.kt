package com.snurdy.backend.domain.inquiry.repository

import com.snurdy.backend.domain.inquiry.entity.InquiryAnswerEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface InquiryAnswerRepository : JpaRepository<InquiryAnswerEntity, Long> {

    fun findByInquiryId(inquiryId: Long): InquiryAnswerEntity?

    fun findAllByInquiryIdIn(inquiryIds: Collection<Long>): List<InquiryAnswerEntity>

    fun existsByInquiryId(inquiryId: Long): Boolean

    @Query(
        """
        SELECT COUNT(a)
          FROM InquiryAnswerEntity a, InquiryEntity i
         WHERE a.inquiryId = i.id
           AND i.userId = :userId
           AND i.deletedAt IS NULL
           AND a.readAt IS NULL
        """
    )
    fun countUnreadAnswersByUserId(@Param("userId") userId: UUID): Long
}