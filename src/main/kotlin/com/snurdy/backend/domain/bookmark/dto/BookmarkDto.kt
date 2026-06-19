package com.snurdy.backend.domain.bookmark.dto

import com.snurdy.backend.domain.bookmark.entity.BookmarkEntity
import org.springframework.data.domain.Page
import java.time.Instant

data class BookmarkPageResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<BookmarkItem>
)

data class BookmarkItem(
    val glossaryId: Long,
    val termId: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val createdAt: Instant?
)

data class BookmarkMutationResponse(
    val glossaryId: Long?,
    val termId: Long,
    val bookmarked: Boolean
)

fun BookmarkEntity.toResponse() = BookmarkItem(
    glossaryId = id.glossaryId,
    termId = id.termId,
    spelling = term.spelling,
    meaning = term.meaning,
    abbreviation = term.abbreviation,
    createdAt = createdAt
)

fun Page<BookmarkEntity>.toResponse() = BookmarkPageResponse(
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    items = content.map { it.toResponse() }
)
