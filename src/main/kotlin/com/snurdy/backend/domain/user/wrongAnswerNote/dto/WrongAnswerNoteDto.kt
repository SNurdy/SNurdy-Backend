package com.snurdy.backend.domain.user.wrongAnswerNote.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

// 오답노트 기간 필터. 카드와 상세 endpoint 양쪽에서 사용
enum class WrongAnswerPeriod {
    TODAY,
    WEEK,
    ALL
}

@Schema(description = "기간별 오답노트 카드 1건")
data class WrongAnswerPeriodCard(
    @field:Schema(description = "기간 식별자", example = "TODAY")
    val period: WrongAnswerPeriod,
    @field:Schema(description = "카드 상단 라벨", example = "오늘 틀린 단어")
    val label: String,
    @field:Schema(description = "해당 기간 안의 오답 단어 수", example = "12")
    val incorrectTermCount: Long
)

@Schema(description = "기간별 오답노트 카드 목록")
data class WrongAnswerPeriodCardListResponse(
    val periods: List<WrongAnswerPeriodCard>
)

@Schema(description = "과별 오답노트 카드 1건")
data class WrongAnswerGlossaryCard(
    @field:Schema(description = "DEFAULT 단어장 ID", example = "1")
    val glossaryId: Long,
    @field:Schema(description = "단어장 제목", example = "정형외과")
    val title: String,
    @field:Schema(description = "단어장 분류", example = "외과")
    val category: String,
    @field:Schema(description = "해당 과의 오답 단어 수", example = "12")
    val incorrectTermCount: Long
)

@Schema(description = "과별 오답노트 카드 목록")
data class WrongAnswerGlossaryCardListResponse(
    val glossaries: List<WrongAnswerGlossaryCard>
)

@Schema(description = "오답 단어 1건")
data class WrongAnswerTermItem(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    @field:Schema(description = "단어의 누적 오답 횟수 (사용자 x 단어 단위)", example = "18")
    val totalIncorrectCount: Int
)

@Schema(description = "오답 단어 페이지 응답")
data class WrongAnswerTermsResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<WrongAnswerTermItem>
)

@Schema(description = "오답노트에서 단어 다중 삭제 요청. 입력한 단어가 모두 노트에 활성 상태로 있어야 처리")
data class DeleteTermsFromWrongAnswerNoteRequest(
    @field:NotEmpty(message = "삭제할 단어 ID 목록이 비어 있을 수 없습니다")
    @field:Size(max = 100, message = "한 번에 최대 100개까지 삭제할 수 있습니다")
    @field:Schema(
        description = "삭제할 단어 ID 목록 (최대 100개)",
        example = "[123, 456]"
    )
    val termIds: List<@Positive Long>
)
