package com.snurdy.backend.domain.admin.dictionary.service

import com.snurdy.backend.domain.admin.dictionary.dto.GlossaryItem
import com.snurdy.backend.domain.admin.dictionary.dto.GlossaryPageResponse
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminGlossaryQueryService(
    private val glossaryRepository: GlossaryRepository
) {

    @Transactional(readOnly = true)
    fun getGlossaries(keyword: String?, page: Int, size: Int): GlossaryPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        val data = if (keyword.isNullOrBlank()) {
            glossaryRepository.findAll(pageable)
        } else {
            val q = keyword.trim()
            glossaryRepository.findByCategoryContainingIgnoreCaseOrTitleContainingIgnoreCase(q, q, pageable)
        }

        return GlossaryPageResponse(
            page = data.number,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages,
            items = data.content.map {
                GlossaryItem(
                    id = it.id ?: throw ApiException(CommonError.INTERNAL_ERROR),
                    category = it.category,
                    title = it.title
                )
            }
        )
    }
}
