package com.snurdy.backend.domain.bookmark.dto

import java.time.Instant

data class BookmarkListResponse(
    val bookmarks: List<BookmarkItem>
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
