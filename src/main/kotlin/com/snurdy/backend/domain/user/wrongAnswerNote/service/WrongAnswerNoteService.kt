package com.snurdy.backend.domain.user.wrongAnswerNote.service

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryType
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.DeleteTermsFromWrongAnswerNoteRequest
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerGlossaryCard
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerGlossaryCardListResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriod
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriodCard
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriodCardListResponse
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerTermItem
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerTermsResponse
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
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class WrongAnswerNoteService(
    private val wrongAnswerTermRepository: WrongAnswerTermRepository,
    private val glossaryRepository: GlossaryRepository,
    private val clock: Clock
) {

    // 학습 submit 시 오답 1건을 오답노트에 누적 반영. soft delete 상태였다면 복구하면서 누적값은 보존
    @Transactional
    fun recordIncorrect(userId: UUID, termId: Long, sourceGlossaryId: Long, occurredAt: Instant) {
        val id = WrongAnswerTermId(userId = userId, termId = termId)
        val existing = wrongAnswerTermRepository.findByIdOrNull(id)
        if (existing == null) {
            wrongAnswerTermRepository.save(
                WrongAnswerTermEntity.firstIncorrect(
                    userId = userId,
                    termId = termId,
                    sourceGlossaryId = sourceGlossaryId,
                    occurredAt = occurredAt
                )
            )
        } else {
            existing.recordIncorrect(sourceGlossaryId, occurredAt)
        }
    }

    @Transactional(readOnly = true)
    fun listByPeriod(userId: UUID): WrongAnswerPeriodCardListResponse {
        val now = clock.instant()
        val cards = WrongAnswerPeriod.entries.map { period ->
            WrongAnswerPeriodCard(
                period = period,
                label = labelOf(period),
                incorrectTermCount = wrongAnswerTermRepository.countActive(userId, thresholdOf(period, now))
            )
        }
        return WrongAnswerPeriodCardListResponse(periods = cards)
    }

    @Transactional(readOnly = true)
    fun listByGlossary(userId: UUID): WrongAnswerGlossaryCardListResponse {
        val groups = wrongAnswerTermRepository.groupBySourceGlossary(userId)
        if (groups.isEmpty()) return WrongAnswerGlossaryCardListResponse(glossaries = emptyList())

        val glossaryIds = groups.map { it.sourceGlossaryId }
        val glossaries = glossaryRepository.findAllById(glossaryIds)
            .filter { it.type == GlossaryType.DEFAULT }
            .associateBy { it.id }

        val cards = groups
            .mapNotNull { group ->
                val glossary = glossaries[group.sourceGlossaryId] ?: return@mapNotNull null
                WrongAnswerGlossaryCard(
                    glossaryId = glossary.id,
                    title = glossary.title,
                    category = glossary.category,
                    incorrectTermCount = group.incorrectTermCount
                )
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
        val after = thresholdOf(period, now)
        return paginatedTerms(userId, after = after, sourceGlossaryId = null, page = page, size = size, query = query)
    }

    @Transactional(readOnly = true)
    fun listGlossaryTerms(
        userId: UUID,
        glossaryId: Long,
        page: Int,
        size: Int,
        query: String?
    ): WrongAnswerTermsResponse {
        val glossary = glossaryRepository.findByIdOrNull(glossaryId)
            ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        if (glossary.type != GlossaryType.DEFAULT) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
        return paginatedTerms(userId, after = null, sourceGlossaryId = glossaryId, page = page, size = size, query = query)
    }

    @Transactional
    fun deleteTerms(userId: UUID, request: DeleteTermsFromWrongAnswerNoteRequest) {
        val distinctTermIds = request.termIds.distinct()

        val existing = wrongAnswerTermRepository
            .findAllByIdUserIdAndIdTermIdInAndDeletedAtIsNull(userId, distinctTermIds)
            .mapTo(mutableSetOf()) { it.id.termId }

        // 단일 삭제와 일관: 하나라도 활성 row 가 없으면 전체 거부
        if (existing.size != distinctTermIds.size) {
            throw ApiException(WrongAnswerNoteError.TERM_NOT_IN_NOTE)
        }

        wrongAnswerTermRepository.softDeleteByUserIdAndTermIds(
            userId = userId,
            termIds = distinctTermIds,
            now = clock.instant()
        )
    }

    private fun paginatedTerms(
        userId: UUID,
        after: Instant?,
        sourceGlossaryId: Long?,
        page: Int,
        size: Int,
        query: String?
    ): WrongAnswerTermsResponse {
        val pageable = PageRequest.of(page, size)
        val trimmedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val result: Page<WrongAnswerTermItemProjection> = if (trimmedQuery == null) {
            wrongAnswerTermRepository.findActiveTerms(userId, after, sourceGlossaryId, pageable)
        } else {
            wrongAnswerTermRepository.searchActiveTerms(userId, after, sourceGlossaryId, trimmedQuery, pageable)
        }
        return WrongAnswerTermsResponse(
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
            terms = result.content.map {
                WrongAnswerTermItem(
                    id = it.id,
                    spelling = it.spelling,
                    meaning = it.meaning,
                    abbreviation = it.abbreviation,
                    totalIncorrectCount = it.totalIncorrectCount
                )
            }
        )
    }

    private fun thresholdOf(period: WrongAnswerPeriod, now: Instant): Instant? = when (period) {
        WrongAnswerPeriod.TODAY -> LocalDate.now(now.atZone(APP_ZONE_ID).zone)
            .atStartOfDay(APP_ZONE_ID).toInstant()
        WrongAnswerPeriod.WEEK -> now.minus(java.time.Duration.ofDays(7))
        WrongAnswerPeriod.ALL -> null
    }

    private fun labelOf(period: WrongAnswerPeriod): String = when (period) {
        WrongAnswerPeriod.TODAY -> "오늘 틀린 단어"
        WrongAnswerPeriod.WEEK -> "이번 주 틀린 단어"
        WrongAnswerPeriod.ALL -> "전체 틀린 단어"
    }

    companion object {
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
