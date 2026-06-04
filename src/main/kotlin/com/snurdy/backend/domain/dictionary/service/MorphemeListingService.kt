package com.snurdy.backend.domain.dictionary.service

import com.snurdy.backend.domain.dictionary.dto.TermSearchResponse
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeListItem
import com.snurdy.backend.domain.dictionary.morpheme.dto.MorphemeListResponse
import com.snurdy.backend.domain.dictionary.morpheme.dto.toListItem
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// 사전 화면 어근어미 모드 책임
// 어근어미 카탈로그 목록과 특정 어근어미가 포함된 단어 목록 두 가지 진입점을 제공한다
@Service
class MorphemeListingService(
    private val morphemeRepository: MorphemeRepository,
    private val termSearchService: TermSearchService
) {

    @Transactional(readOnly = true)
    fun listMorphemes(query: String?, page: Int, size: Int): MorphemeListResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "spelling"))
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }

        val result = if (normalizedQuery == null) {
            morphemeRepository.findAll(pageable)
        } else {
            morphemeRepository.findBySpellingContainingIgnoreCaseOrMeaningContainingIgnoreCase(
                normalizedQuery, normalizedQuery, pageable
            )
        }

        return MorphemeListResponse(
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
            morphemes = result.content.map { it.toListItem() }
        )
    }

    @Transactional(readOnly = true)
    fun listTermsByMorpheme(
        userId: UUID,
        morphemeId: Long,
        category: String?,
        query: String?,
        page: Int,
        size: Int
    ): TermSearchResponse {
        if (!morphemeRepository.existsById(morphemeId)) {
            throw ApiException(DictionaryError.MORPHEME_NOT_FOUND)
        }
        return termSearchService.searchTerms(
            userId = userId,
            query = query,
            category = category,
            glossaryId = null,
            morphemeId = morphemeId,
            page = page,
            size = size
        )
    }
}
