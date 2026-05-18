package com.snurdy.backend.domain.bookmark.dto

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
