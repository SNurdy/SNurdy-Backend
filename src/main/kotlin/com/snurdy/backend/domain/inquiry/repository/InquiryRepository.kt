package com.snurdy.backend.domain.inquiry.repository

import com.snurdy.backend.domain.inquiry.entity.InquiryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InquiryRepository : JpaRepository<InquiryEntity, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): InquiryEntity?

    fun findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId: UUID): List<InquiryEntity>
}