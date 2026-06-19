package com.snurdy.backend.domain.user.customNote.dto

import com.snurdy.backend.domain.user.customNote.entity.CustomNoteEntity
import io.swagger.v3.oas.annotations.media.Schema

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

// 합성형 응답이며 service 가 외부 source 를 인자로 조립해 호출한다
// shareCode 는 호출자가 null 검증해 non-null 로 전달해야 한다
fun CustomNoteEntity.toShareIssueResponse(
    shareCode: String,
    ownerNickname: String?,
    termCount: Long,
    shareUrl: String
): CustomNoteShareIssueResponse = CustomNoteShareIssueResponse(
    noteId = id,
    title = title,
    ownerNickname = ownerNickname,
    termCount = termCount,
    shareCode = shareCode,
    shareUrl = shareUrl
)
