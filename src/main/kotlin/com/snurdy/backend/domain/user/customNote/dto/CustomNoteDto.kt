package com.snurdy.backend.domain.user.customNote.dto

import com.snurdy.backend.domain.user.customNote.entity.CustomNoteEntity
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteTermProjection
import com.snurdy.backend.global.validation.CodePointSize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant

@Schema(description = "커스텀 단어장 생성 요청")
data class CreateCustomNoteRequest(
    @field:NotBlank(message = "단어장 제목은 필수입니다")
    @field:CodePointSize(max = 14, message = "단어장 제목은 최대 14자까지 입력할 수 있습니다")
    @field:Schema(description = "단어장 제목", example = "외과 단어장")
    val title: String
)

@Schema(description = "커스텀 단어장 수정 요청")
data class UpdateCustomNoteRequest(
    @field:CodePointSize(min = 1, max = 14, message = "단어장 제목은 1~14자여야 합니다")
    @field:Schema(description = "변경할 단어장 제목", example = "신경계 단어장")
    val title: String
)

@Schema(description = "단어장에 단어 추가 요청. 단어가 어느 단어장에서 선택되었는지 (glossaryId) 와 termId 쌍")
data class AddTermsToCustomNoteRequest(
    @field:NotEmpty(message = "추가할 단어 항목이 비어 있을 수 없습니다")
    @field:Size(max = 100, message = "한 번에 최대 100개까지 추가할 수 있습니다")
    @field:Valid
    val items: List<CustomNoteTermInput>
)

@Schema(description = "추가할 단어 항목 (단어가 속한 단어장 id + 단어 id)")
data class CustomNoteTermInput(
    @field:NotNull
    @field:Positive
    @field:Schema(description = "단어가 선택된 단어장 id", example = "12")
    val glossaryId: Long,

    @field:NotNull
    @field:Positive
    @field:Schema(description = "단어 id", example = "101")
    val termId: Long
)

@Schema(description = "커스텀 단어장 요약")
data class CustomNoteResponse(
    @field:Schema(description = "커스텀 단어장 id", example = "3")
    val customNoteId: Long,
    val title: String,
    val termCount: Long,
    val createdAt: Instant?
)

@Schema(description = "커스텀 단어장 목록 응답")
data class CustomNoteListResponse(
    val customNotes: List<CustomNoteResponse>
)

@Schema(description = "단어 추가 결과")
data class AddTermsToCustomNoteResponse(
    @field:Schema(description = "실제로 새로 추가된 단어 수")
    val added: Int,
    @field:Schema(description = "이미 단어장에 있어 건너뛴 단어 수")
    val skipped: Int,
    @field:Schema(description = "추가 후 단어장의 총 단어 수")
    val totalTermCount: Long
)

@Schema(description = "사전 단어 1개를 N개 커스텀 단어장에 동시에 추가 요청")
data class AddTermToCustomNotesRequest(
    @field:NotNull
    @field:Positive
    @field:Schema(description = "단어가 선택된 단어장 id", example = "12")
    val glossaryId: Long,

    @field:NotNull
    @field:Positive
    @field:Schema(description = "단어 id", example = "101")
    val termId: Long,

    @field:NotEmpty(message = "추가할 단어장 ID 목록이 비어 있을 수 없습니다")
    @field:Size(max = 20, message = "한 번에 최대 20개 단어장까지 추가할 수 있습니다")
    @field:Schema(
        description = "단어를 추가할 커스텀 단어장 ID 목록. 모두 본인 소유여야 처리",
        example = "[1, 5, 12]"
    )
    val customNoteIds: List<@Positive Long>
)

@Schema(description = "단어 기준 커스텀 단어장 보유 현황 응답. 북마크 뷰 진입 시 사용")
data class CustomNotesForTermResponse(
    val customNotes: List<CustomNoteForTermItem>
)

@Schema(description = "단어 보유 여부 포함 단어장 항목")
data class CustomNoteForTermItem(
    @field:Schema(description = "커스텀 단어장 id", example = "3")
    val customNoteId: Long,
    @field:Schema(description = "단어장 제목", example = "외과 단어장")
    val title: String,
    @field:Schema(description = "단어장의 현재 단어 수. 250 도달 시 클라가 추가 버튼 disabled", example = "50")
    val termCount: Long,
    @field:Schema(description = "이 단어가 단어장에 이미 들어 있는지 여부", example = "false")
    val hasTerm: Boolean
)

@Schema(description = "단어를 N개 단어장에 추가한 결과")
data class AddTermToCustomNotesResponse(
    @field:Schema(description = "새로 추가된 단어장 ID 목록", example = "[1, 12]")
    val addedCustomNoteIds: List<Long>
)

@Schema(description = "단어장에서 단어 다중 삭제 요청. 입력한 (glossaryId, termId) 쌍이 모두 단어장에 있어야 처리")
data class DeleteTermsFromCustomNoteRequest(
    @field:NotEmpty(message = "삭제할 단어 항목이 비어 있을 수 없습니다")
    @field:Size(max = 100, message = "한 번에 최대 100개까지 삭제할 수 있습니다")
    @field:Valid
    val items: List<CustomNoteTermInput>
)

@Schema(description = "단어장 다중 삭제 요청. 모두 본인 소유여야 처리")
data class DeleteCustomNotesRequest(
    @field:NotEmpty(message = "삭제할 단어장 ID 목록이 비어 있을 수 없습니다")
    @field:Size(max = 20, message = "한 번에 최대 20개까지 삭제할 수 있습니다")
    @field:Schema(
        description = "삭제할 단어장 ID 목록 (최대 20개)",
        example = "[7, 12]"
    )
    val customNoteIds: List<@Positive Long>
)

@Schema(description = "단어장 내 단어 페이지 응답")
data class CustomNoteTermsResponse(
    val customNoteId: Long,
    val title: String,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<CustomNoteTermItem>
)

@Schema(description = "단어장 내 단어 항목")
data class CustomNoteTermItem(
    @field:Schema(description = "단어 id", example = "101")
    val termId: Long,
    @field:Schema(description = "단어가 속한 단어장 id", example = "12")
    val glossaryId: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?
)

fun CustomNoteEntity.toResponse(termCount: Long) = CustomNoteResponse(
    customNoteId = id,
    title = title,
    termCount = termCount,
    createdAt = createdAt
)

fun CustomNoteTermProjection.toResponse() = CustomNoteTermItem(
    termId = termId,
    glossaryId = glossaryId,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation
)
