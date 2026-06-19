package com.snurdy.backend.domain.bookmark.controller

import com.snurdy.backend.domain.bookmark.dto.BookmarkMutationResponse
import com.snurdy.backend.domain.bookmark.dto.BookmarkPageResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Tag(name = "Bookmark", description = "북마크 API")
@RequestMapping("/v1/bookmarks")
interface BookmarkApi {

    @Operation(summary = "북마크 목록 조회")
    @ApiSuccessCode("BOOKMARK_S001")
    @GetMapping
    fun getBookmarks(
        @CurrentUserId userId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ApiResponse<BookmarkPageResponse>

    @Operation(summary = "북마크 추가")
    @ApiSuccessCode("BOOKMARK_S002")
    @ApiErrorCodes("USER_E010", "DICTIONARY_E001", "DICTIONARY_E002", "DICTIONARY_E004")
    @PutMapping("/{termId}")
    fun addBookmark(
        @CurrentUserId userId: UUID,
        @PathVariable termId: Long,
        @RequestParam glossaryId: Long
    ): ApiResponse<BookmarkMutationResponse>

    @Operation(summary = "북마크 삭제")
    @ApiSuccessCode("BOOKMARK_S003")
    @DeleteMapping("/{termId}")
    fun removeBookmark(
        @CurrentUserId userId: UUID,
        @PathVariable termId: Long,
        @RequestParam glossaryId: Long
    ): ApiResponse<BookmarkMutationResponse>
}
