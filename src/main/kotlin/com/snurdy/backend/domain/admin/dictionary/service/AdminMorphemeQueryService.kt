package com.snurdy.backend.domain.admin.dictionary.service

import com.snurdy.backend.domain.admin.dictionary.dto.MorphemeItem
import com.snurdy.backend.domain.admin.dictionary.dto.MorphemePageResponse
import com.snurdy.backend.domain.admin.dictionary.dto.MorphemeVariantItem
import com.snurdy.backend.domain.admin.dictionary.dto.MorphemeVariantPageResponse
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminMorphemeQueryService(
    private val morphemeRepository: MorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository
) {

    @Transactional(readOnly = true)
    fun getMorphemes(keyword: String?, page: Int, size: Int): MorphemePageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        val data = if (keyword.isNullOrBlank()) {
            morphemeRepository.findAll(pageable)
        } else {
            val q = keyword.trim()
            morphemeRepository.findBySpellingContainingIgnoreCaseOrMeaningContainingIgnoreCase(q, q, pageable)
        }

        return MorphemePageResponse(
            page = data.number,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages,
            items = data.content.map {
                MorphemeItem(
                    id = it.id,
                    spelling = it.spelling,
                    meaning = it.meaning
                )
            }
        )
    }

    @Transactional(readOnly = true)
    fun getMorphemeVariants(keyword: String?, morphemeId: Long?, page: Int, size: Int): MorphemeVariantPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        val data = when {
            morphemeId != null -> morphemeVariantRepository.findByMorphemeId(morphemeId, pageable)
            keyword.isNullOrBlank() -> morphemeVariantRepository.findAll(pageable)
            else -> {
                val q = keyword.trim()
                morphemeVariantRepository.findByVariantContainingIgnoreCaseOrBaseContainingIgnoreCase(q, q, pageable)
            }
        }

        return MorphemeVariantPageResponse(
            page = data.number,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages,
            items = data.content.map {
                MorphemeVariantItem(
                    id = it.id,
                    morphemeId = it.morpheme.id,
                    variant = it.variant,
                    base = it.base
                )
            }
        )
    }
}
