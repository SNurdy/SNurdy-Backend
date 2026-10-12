package com.snurdy.backend.domain.inquiry.admin.service

import com.snurdy.backend.domain.inquiry.admin.dto.CreateInquiryAnswerRequest
import com.snurdy.backend.domain.inquiry.admin.dto.toEntity
import com.snurdy.backend.domain.inquiry.dto.InquiryAnswerResponse
import com.snurdy.backend.domain.inquiry.dto.toResponse
import com.snurdy.backend.domain.inquiry.repository.InquiryAnswerRepository
import com.snurdy.backend.domain.inquiry.repository.InquiryRepository
import com.snurdy.backend.global.constants.InquiryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AdminInquiryService(
    private val inquiryRepository: InquiryRepository,
    private val inquiryAnswerRepository: InquiryAnswerRepository
) {

    @Transactional
    fun createInquiryAnswer(
        adminUserId: UUID,
        inquiryId: Long,
        request: CreateInquiryAnswerRequest
    ): InquiryAnswerResponse {
        val inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
            ?: throw ApiException(InquiryError.NOT_FOUND)
        if (inquiry.isAnswered()) throw ApiException(InquiryError.ANSWER_ALREADY_EXISTS)

        val answer = inquiryAnswerRepository.save(request.toEntity(inquiryId, adminUserId))
        inquiry.markAnswered()
        return answer.toResponse()
    }
}