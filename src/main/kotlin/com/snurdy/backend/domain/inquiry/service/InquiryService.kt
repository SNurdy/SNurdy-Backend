package com.snurdy.backend.domain.inquiry.service

import com.snurdy.backend.domain.inquiry.dto.CreateInquiryRequest
import com.snurdy.backend.domain.inquiry.dto.InquiryDetailResponse
import com.snurdy.backend.domain.inquiry.dto.InquiryListResponse
import com.snurdy.backend.domain.inquiry.dto.UpdateInquiryRequest
import com.snurdy.backend.domain.inquiry.dto.toDetail
import com.snurdy.backend.domain.inquiry.dto.toEntity
import com.snurdy.backend.domain.inquiry.dto.toSummary
import com.snurdy.backend.domain.inquiry.entity.InquiryEntity
import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailOutboxEntity
import com.snurdy.backend.domain.inquiry.mail.entity.InquiryMailType
import com.snurdy.backend.domain.inquiry.mail.event.InquiryMailRequestedEvent
import com.snurdy.backend.domain.inquiry.mail.repository.InquiryMailOutboxRepository
import com.snurdy.backend.domain.inquiry.repository.InquiryAnswerRepository
import com.snurdy.backend.domain.inquiry.repository.InquiryRepository
import com.snurdy.backend.global.constants.InquiryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class InquiryService(
    private val inquiryRepository: InquiryRepository,
    private val inquiryAnswerRepository: InquiryAnswerRepository,
    private val inquiryMailOutboxRepository: InquiryMailOutboxRepository,
    private val inquiryRateLimiter: InquiryRateLimiter,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock
) {

    @Transactional
    fun createInquiry(userId: UUID, request: CreateInquiryRequest): InquiryDetailResponse {
        inquiryRateLimiter.checkMailSendingAllowed(userId)
        val inquiry = inquiryRepository.save(request.toEntity(userId))
        requestInquiryMail(inquiry, InquiryMailType.CREATED)
        return inquiry.toDetail(answer = null)
    }

    @Transactional(readOnly = true)
    fun listInquiries(userId: UUID): InquiryListResponse =
        InquiryListResponse(
            inquiries = inquiryRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .map { it.toSummary() }
        )

    @Transactional(readOnly = true)
    fun getInquiry(userId: UUID, inquiryId: Long): InquiryDetailResponse {
        val inquiry = inquiryRepository.findByIdAndUserIdAndDeletedAtIsNull(inquiryId, userId)
            ?: throw ApiException(InquiryError.NOT_FOUND)
        val answer = inquiryAnswerRepository.findByInquiryId(inquiry.id)
        return inquiry.toDetail(answer)
    }

    @Transactional
    fun updateInquiry(userId: UUID, inquiryId: Long, request: UpdateInquiryRequest): InquiryDetailResponse {
        val inquiry = inquiryRepository.findByIdAndUserIdAndDeletedAtIsNull(inquiryId, userId)
            ?: throw ApiException(InquiryError.NOT_FOUND)
        if (inquiry.isAnswered()) throw ApiException(InquiryError.ALREADY_ANSWERED)

        val content = request.content.trim()
        // 내용이 같으면 수정 메일이 중복으로 가지 않도록 아무것도 하지 않는다
        if (inquiry.hasSameCategoryAndContent(request.category, content)) {
            return inquiry.toDetail(answer = null)
        }

        inquiryRateLimiter.checkMailSendingAllowed(userId)
        inquiry.updateCategoryAndContent(request.category, content)
        flushWithAnswerConflictCheck()
        requestInquiryMail(inquiry, InquiryMailType.UPDATED)
        return inquiry.toDetail(answer = null)
    }

    @Transactional
    fun deleteInquiry(userId: UUID, inquiryId: Long) {
        val inquiry = inquiryRepository.findByIdAndUserIdAndDeletedAtIsNull(inquiryId, userId)
            ?: throw ApiException(InquiryError.NOT_FOUND)
        if (inquiry.isAnswered()) throw ApiException(InquiryError.ALREADY_ANSWERED)

        inquiry.softDelete(clock.instant())
        flushWithAnswerConflictCheck()
    }

    // 관리자 답변과 동시에 커밋되면 version 충돌이 커밋 시점에 나므로 미리 flush 해 문의 도메인 에러로 바꾼다
    private fun flushWithAnswerConflictCheck() {
        try {
            inquiryRepository.flush()
        } catch (e: OptimisticLockingFailureException) {
            throw ApiException(InquiryError.ALREADY_ANSWERED, cause = e)
        }
    }

    private fun requestInquiryMail(inquiry: InquiryEntity, mailType: InquiryMailType) {
        val outbox = inquiryMailOutboxRepository.save(
            InquiryMailOutboxEntity(
                inquiryId = inquiry.id,
                mailType = mailType,
                category = inquiry.category,
                content = inquiry.content
            )
        )
        eventPublisher.publishEvent(InquiryMailRequestedEvent(outbox.id))
    }
}