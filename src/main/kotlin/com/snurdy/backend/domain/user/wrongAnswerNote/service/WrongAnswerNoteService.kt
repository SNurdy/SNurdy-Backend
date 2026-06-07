package com.snurdy.backend.domain.user.wrongAnswerNote.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.DeleteTermsFromWrongAnswerNoteRequest
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerGlossaryCardListResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriod
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriodCard
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.thresholdAfter
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriodCardListResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerTermsResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.toResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.toWrongAnswerCard
import com.snurdy.backend.domain.user.wrongAnswerNote.entity.WrongAnswerTermEntity
import com.snurdy.backend.domain.user.wrongAnswerNote.entity.WrongAnswerTermId
import com.snurdy.backend.domain.user.wrongAnswerNote.repository.WrongAnswerTermItemProjection
import com.snurdy.backend.domain.user.wrongAnswerNote.repository.WrongAnswerTermRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.WrongAnswerNoteError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class WrongAnswerNoteService(
    private val wrongAnswerTermRepository: WrongAnswerTermRepository,
    private val glossaryRepository: GlossaryRepository,
    private val clock: Clock
) {

    // 학습 submit 시 오답 1건을 오답노트에 누적 반영. soft delete 상태였다면 복구하면서 누적값은 보존
    @Transactional
    fun recordIncorrect(userId: UUID, termId: Long, glossaryId: Long, occurredAt: Instant) {
        val id = WrongAnswerTermId(userId = userId, termId = termId)
        val existing = wrongAnswerTermRepository.findByIdOrNull(id)
        if (existing == null) {
            wrongAnswerTermRepository.save(
                WrongAnswerTermEntity.firstIncorrect(
                    userId = userId,
                    termId = termId,
                    glossaryId = glossaryId,
                    occurredAt = occurredAt
                )
            )
        } else {
            existing.recordIncorrect(glossaryId, occurredAt)
        }
    }

    @Transactional(readOnly = true)
    fun listByPeriod(userId: UUID): WrongAnswerPeriodCardListResponse {
        val now = clock.instant()
        val cards = WrongAnswerPeriod.entries.map { period ->
            WrongAnswerPeriodCard(
                period = period,
                label = labelOf(period),
                incorrectTermCount = wrongAnswerTermRepository.countActive(userId, period.thresholdAfter(now))
            )
        }
        return WrongAnswerPeriodCardListResponse(periods = cards)
    }

    @Transactional(readOnly = true)
    fun listByGlossary(userId: UUID): WrongAnswerGlossaryCardListResponse {
        val groups = wrongAnswerTermRepository.groupByGlossary(userId)
        if (groups.isEmpty()) return WrongAnswerGlossaryCardListResponse(glossaries = emptyList())

        val glossaryIds = groups.map { it.glossaryId }
        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id }

        val cards = groups
            .mapNotNull { group ->
                val glossary = glossaries[group.glossaryId] ?: return@mapNotNull null
                glossary.toWrongAnswerCard(group.incorrectTermCount)
            }
            .sortedBy { it.glossaryId }
        return WrongAnswerGlossaryCardListResponse(glossaries = cards)
    }

    @Transactional(readOnly = true)
    fun listPeriodTerms(
        userId: UUID,
        period: WrongAnswerPeriod,
        page: Int,
        size: Int,
        query: String?
    ): WrongAnswerTermsResponse {
        val now = clock.instant()
        val after = period.thresholdAfter(now)
        return paginatedTerms(userId, after = after, glossaryId = null, page = page, size = size, query = query)
    }

    @Transactional(readOnly = true)
    fun listGlossaryTerms(
        userId: UUID,
        glossaryId: Long,
        page: Int,
        size: Int,
        query: String?
    ): WrongAnswerTermsResponse {
        glossaryRepository.findByIdOrNull(glossaryId)
            ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        return paginatedTerms(userId, after = null, glossaryId = glossaryId, page = page, size = size, query = query)
    }

    @Transactional
    fun deleteTerms(userId: UUID, request: DeleteTermsFromWrongAnswerNoteRequest) {
        val distinctTermIds = request.termIds.distinct()

        val activeEntities = wrongAnswerTermRepository
            .findAllByIdUserIdAndIdTermIdInAndDeletedAtIsNull(userId, distinctTermIds)

        // 단일 삭제와 일관: 하나라도 활성 row 가 없으면 전체 거부
        if (activeEntities.size != distinctTermIds.size) {
            throw ApiException(WrongAnswerNoteError.TERM_NOT_IN_NOTE)
        }

        val now = clock.instant()
        activeEntities.forEach { it.softDelete(now) }
    }

    private fun paginatedTerms(
        userId: UUID,
        after: Instant?,
        glossaryId: Long?,
        page: Int,
        size: Int,
        query: String?
    ): WrongAnswerTermsResponse {
        val pageable = PageRequest.of(page, size)
        val trimmedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val result: Page<WrongAnswerTermItemProjection> = if (trimmedQuery == null) {
            wrongAnswerTermRepository.findActiveTerms(userId, after, glossaryId, pageable)
        } else {
            wrongAnswerTermRepository.searchActiveTerms(userId, after, glossaryId, trimmedQuery, pageable)
        }
        return WrongAnswerTermsResponse(
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
            terms = result.content.map { it.toResponse() }
        )
    }

    private fun labelOf(period: WrongAnswerPeriod): String = when (period) {
        WrongAnswerPeriod.TODAY -> "오늘 틀린 단어"
        WrongAnswerPeriod.WEEK -> "이번 주 틀린 단어"
        WrongAnswerPeriod.MONTH -> "이번 달 틀린 단어"
        WrongAnswerPeriod.ALL -> "전체 틀린 단어"
    }
}
