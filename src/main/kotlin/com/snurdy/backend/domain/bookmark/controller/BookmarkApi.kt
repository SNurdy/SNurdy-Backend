package com.snurdy.backend.domain.bookmark.controller

import com.snurdy.backend.domain.bookmark.dto.BookmarkListResponse
import com.snurdy.backend.domain.bookmark.dto.BookmarkMutationResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Bookmark", description = "북마크 API")
interface BookmarkApi {

    @Operation(summary = "북마크 목록 조회")
    @GetMapping("/v1/bookmarks")
    fun getBookmarks(): ApiResponse<BookmarkListResponse>

    @Operation(summary = "북마크 추가")
    @PutMapping("/v1/bookmarks/{termId}")
    fun addBookmark(
        @PathVariable termId: Long,
        @RequestParam(required = false) glossaryId: Long?
    ): ApiResponse<BookmarkMutationResponse>

    @Operation(summary = "북마크 삭제")
    @DeleteMapping("/v1/bookmarks/{termId}")
    fun removeBookmark(
        @PathVariable termId: Long,
        @RequestParam(required = false) glossaryId: Long?
    ): ApiResponse<BookmarkMutationResponse>
}
