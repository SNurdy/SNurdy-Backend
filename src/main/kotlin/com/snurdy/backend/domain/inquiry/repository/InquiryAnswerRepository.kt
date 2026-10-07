package com.snurdy.backend.domain.inquiry.repository

import com.snurdy.backend.domain.inquiry.entity.InquiryAnswerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface InquiryAnswerRepository : JpaRepository<InquiryAnswerEntity, Long> {

    fun findByInquiryId(inquiryId: Long): InquiryAnswerEntity?

    fun existsByInquiryId(inquiryId: Long): Boolean
}