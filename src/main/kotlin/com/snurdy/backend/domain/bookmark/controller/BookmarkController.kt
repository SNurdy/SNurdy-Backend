package com.snurdy.backend.domain.bookmark.controller

import com.snurdy.backend.domain.bookmark.dto.BookmarkListResponse
import com.snurdy.backend.domain.bookmark.dto.BookmarkMutationResponse
import com.snurdy.backend.domain.bookmark.service.BookmarkService
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.extension.currentUserId
import org.springframework.web.bind.annotation.RestController

@RestController
class BookmarkController(
    private val bookmarkService: BookmarkService
) : BookmarkApi {
    override fun getBookmarks(): ApiResponse<BookmarkListResponse> =
        ApiResponse.success(CommonSuccess.OK, bookmarkService.getBookmarks(currentUserId()))

    override fun addBookmark(termId: Long, glossaryId: Long?): ApiResponse<BookmarkMutationResponse> =
        ApiResponse.success(
            CommonSuccess.OK,
            bookmarkService.addBookmark(
                userId = currentUserId(),
                termId = termId,
                glossaryId = glossaryId
            )
        )

    override fun removeBookmark(termId: Long, glossaryId: Long?): ApiResponse<BookmarkMutationResponse> =
        ApiResponse.success(
            CommonSuccess.OK,
            bookmarkService.removeBookmark(
                userId = currentUserId(),
                termId = termId,
                glossaryId = glossaryId
            )
        )
}
