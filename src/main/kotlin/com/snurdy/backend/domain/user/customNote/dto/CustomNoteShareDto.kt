package com.snurdy.backend.domain.user.customNote.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "공유 코드 발급 응답")
data class CustomNoteShareIssueResponse(
    @field:Schema(description = "공유 대상 단어장 ID", example = "12")
    val noteId: Long,
    @field:Schema(description = "단어장 제목", example = "외과 단어장")
    val title: String,
    @field:Schema(description = "단어장 소유자 닉네임", example = "지윤")
    val ownerNickname: String?,
    @field:Schema(description = "단어 수", example = "42")
    val termCount: Long,
    @field:Schema(description = "8자 영숫자 공유 코드", example = "k7Mx9pQ2")
    val shareCode: String,
    @field:Schema(description = "Universal Link URL", example = "https://snurdy.org/shared/k7Mx9pQ2")
    val shareUrl: String
)

@Schema(description = "공유 코드로 조회한 단어장 응답")
data class SharedCustomNoteResponse(
    val shareCode: String,
    val title: String,
    val ownerNickname: String?,
    val termCount: Long,
    val createdAt: Instant?,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<CustomNoteTermItem>
)
