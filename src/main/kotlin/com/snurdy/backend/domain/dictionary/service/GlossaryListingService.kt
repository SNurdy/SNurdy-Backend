package com.snurdy.backend.domain.dictionary.service

import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryListResponse
import com.snurdy.backend.domain.dictionary.dto.ClientGlossarySummary
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermItem
import com.snurdy.backend.domain.dictionary.dto.ClientGlossaryTermsResponse
import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// 단어장 목록 / 단어장 내 단어 목록 책임 전담
// 본인 학습 진행률 + 학습 상태 + 북마크 컨텍스트 결합
@Service
class GlossaryListingService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository
) {

    @Transactional(readOnly = true)
    fun getGlossaries(userId: UUID, category: String?): ClientGlossaryListResponse {
        val normalizedCategory = category?.trim().orEmpty()

        val glossaryProgressMap = glossaryProgressRepository.findAllByIdUserId(userId)
            .associateBy { it.id.glossaryId }

        // 사전 카탈로그 endpoint 라 type=DEFAULT 만 노출한다. CUSTOM/WRONG_ANSWER 는 customNote 도메인 책임
        val glossaries = glossaryRepository.findDefaultGlossaryTermCounts()
            .asSequence()
            .filter {
                normalizedCategory.isBlank() ||
                    it.category.contains(normalizedCategory, ignoreCase = true)
            }
            .map { projection ->
                val learnedCount = glossaryProgressMap[projection.glossaryId]?.learnedCount ?: 0
                val progressRate = GlossaryProgressCalculator.progressRate(learnedCount, projection.termCount)

                ClientGlossarySummary(
                    id = projection.glossaryId,
                    category = projection.category,
                    title = projection.title,
                    termCount = projection.termCount,
                    learnedCount = learnedCount,
                    progressRate = progressRate,
                    level = GlossaryProgressCalculator.levelFromRate(progressRate)
                )
            }
            .toList()

        return ClientGlossaryListResponse(glossaries = glossaries)
    }

    @Transactional(readOnly = true)
    fun getGlossaryTerms(
        userId: UUID,
        glossaryId: Long,
        filter: String,
        page: Int,
        size: Int
    ): ClientGlossaryTermsResponse {
        val glossary = glossaryRepository.findById(glossaryId)
            .orElseThrow { ApiException(DictionaryError.GLOSSARY_NOT_FOUND) }

        val normalizedFilter = normalizeFilter(filter)
        val pageable = PageRequest.of(page, size)

        val pageResult = glossaryTermRepository.findPagedClientGlossaryTerms(
            userId = userId,
            glossaryId = glossaryId,
            filter = normalizedFilter.queryValue,
            pageable = pageable
        )

        return ClientGlossaryTermsResponse(
            glossaryId = glossary.id,
            category = glossary.category,
            title = glossary.title,
            filter = normalizedFilter.responseValue,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext(),
            terms = pageResult.content.map { projection ->
                ClientGlossaryTermItem(
                    id = projection.termId,
                    spelling = projection.spelling,
                    meaning = projection.meaning,
                    abbreviation = projection.abbreviation,
                    status = projection.status,
                    bookmarked = projection.bookmarked == 1
                )
            }
        )
    }

    private fun normalizeFilter(filter: String): TermFilter {
        val value = filter.trim().lowercase()
        return TermFilter.entries.firstOrNull { it.apiValue == value }
            ?: throw ApiException(DictionaryError.INVALID_FILTER)
    }

    private enum class TermFilter(val apiValue: String, val responseValue: String, val queryValue: String) {
        ALL("all", "all", "ALL"),
        LEARNED("learned", "learned", "LEARNED"),
        NOT_LEARNED("notlearned", "notLearned", "NOT_LEARNED")
    }
}
