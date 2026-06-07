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

    // 기본 단어장 submit 시 오답 batch 누적 반영
    // 신규 termId 면 INSERT, 기존 row 면 totalIncorrectCount += 1 + incorrectAt 갱신 + soft delete 복구
    // glossaryId 는 호출자가 결정 (현재 학습 중인 단어장)
    @Transactional
    fun recordIncorrects(userId: UUID, termIds: List<Long>, glossaryId: Long, occurredAt: Instant) {
        if (termIds.isEmpty()) return
        val distinctTermIds = termIds.distinct()

        val existingByTermId = wrongAnswerTermRepository
            .findAllByIdUserIdAndIdTermIdIn(userId, distinctTermIds)
            .associateBy { it.id.termId }

        val newTermIds = distinctTermIds.filter { it !in existingByTermId }
        if (newTermIds.isNotEmpty()) {
            wrongAnswerTermRepository.saveAll(
                newTermIds.map { termId ->
                    WrongAnswerTermEntity.firstIncorrect(
                        userId = userId,
                        termId = termId,
                        glossaryId = glossaryId,
                        occurredAt = occurredAt
                    )
                }
            )
        }
        existingByTermId.values.forEach { it.recordIncorrect(glossaryId, occurredAt) }
    }

    // 오답노트 submit 시 또 틀린 단어들의 누적값만 갱신
    // 입력 termId 는 이미 wrong_answer_terms 에 있다는 전제 (오답노트 카드 학습 흐름)
    // 신규 INSERT 안 함. glossaryId 는 기존 row 값 유지 — 처음 틀린 단어장 보존
    @Transactional
    fun recordIncorrectsAgain(userId: UUID, termIds: List<Long>, occurredAt: Instant) {
        if (termIds.isEmpty()) return
        val rows = wrongAnswerTermRepository
            .findAllByIdUserIdAndIdTermIdIn(userId, termIds.distinct())
        rows.forEach { row ->
            val glossaryId = row.glossaryId ?: return@forEach
            row.recordIncorrect(glossaryId, occurredAt)
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
