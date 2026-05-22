package com.snurdy.backend.domain.user.termMemo.dto

import com.snurdy.backend.domain.user.termMemo.entity.UserTermMemoEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

@Schema(description = "단어 메모 작성/수정 요청 (upsert)")
data class UpsertTermMemoRequest(
    @field:NotBlank(message = "메모 내용이 비어 있을 수 없습니다")
    @field:Size(max = UserTermMemoEntity.MAX_CONTENT_LENGTH, message = "메모는 최대 500자까지 입력할 수 있습니다")
    @field:Schema(description = "메모 본문", example = "혈전 관련 검사 항목")
    val content: String
)

@Schema(description = "단어 메모 응답")
data class TermMemoResponse(
    @field:Schema(description = "메모가 달린 단어 ID", example = "123")
    val termId: Long,
    @field:Schema(description = "메모 본문", example = "혈전 관련 검사 항목")
    val content: String,
    @field:Schema(description = "마지막 갱신 시각")
    val updatedAt: Instant?
) {
    companion object {
        fun from(memo: UserTermMemoEntity): TermMemoResponse = TermMemoResponse(
            termId = memo.id.termId,
            content = memo.content,
            updatedAt = memo.updatedAt
        )
    }
}
