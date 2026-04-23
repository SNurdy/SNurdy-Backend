package com.snurdy.backend.domain.bookmark.controller

import com.snurdy.backend.domain.bookmark.dto.BookmarkListResponse
import com.snurdy.backend.domain.bookmark.dto.BookmarkMutationResponse
import com.snurdy.backend.domain.bookmark.service.BookmarkService
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class BookmarkController(
    private val bookmarkService: BookmarkService
) : BookmarkApi {
    override fun getBookmarks(userId: UUID): ApiResponse<BookmarkListResponse> =
        ApiResponse.success(CommonSuccess.OK, bookmarkService.getBookmarks(userId))

    override fun addBookmark(userId: UUID, termId: Long, glossaryId: Long?): ApiResponse<BookmarkMutationResponse> =
        ApiResponse.success(
            CommonSuccess.OK,
            bookmarkService.addBookmark(userId = userId, termId = termId, glossaryId = glossaryId)
        )

    override fun removeBookmark(userId: UUID, termId: Long, glossaryId: Long?): ApiResponse<BookmarkMutationResponse> =
        ApiResponse.success(
            CommonSuccess.OK,
            bookmarkService.removeBookmark(userId = userId, termId = termId, glossaryId = glossaryId)
        )
}
