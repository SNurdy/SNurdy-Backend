package com.snurdy.backend.domain.user.wrongAnswerNote.dto

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.user.wrongAnswerNote.repository.WrongAnswerTermItemProjection
import com.snurdy.backend.global.constants.APP_ZONE_ID
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// 오답노트 기간 필터. 카드와 상세 endpoint 양쪽에서 사용
// WEEK 는 자연주 (이번 주 월요일 00:00 KST 이후), MONTH 는 자연월 (이번 달 1일 00:00 KST 이후)
enum class WrongAnswerPeriod {
    TODAY,
    WEEK,
    MONTH,
    ALL
}

// 기간 필터의 incorrectAt 하한값. ALL 은 null (무제한)
// WrongAnswerNoteService 와 CardService 양쪽에서 사용
fun WrongAnswerPeriod.thresholdAfter(now: Instant): Instant? {
    val today = LocalDate.ofInstant(now, APP_ZONE_ID)
    return when (this) {
        WrongAnswerPeriod.TODAY -> today.atStartOfDay(APP_ZONE_ID).toInstant()
        WrongAnswerPeriod.WEEK -> today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(APP_ZONE_ID).toInstant()
        WrongAnswerPeriod.MONTH -> today
            .withDayOfMonth(1)
            .atStartOfDay(APP_ZONE_ID).toInstant()
        WrongAnswerPeriod.ALL -> null
    }
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

@Schema(
    description = "기간별 오답노트 카드 목록",
    example = """{"periods":[{"period":"TODAY","label":"오늘 틀린 단어","incorrectTermCount":12},{"period":"WEEK","label":"이번 주 틀린 단어","incorrectTermCount":34},{"period":"MONTH","label":"이번 달 틀린 단어","incorrectTermCount":120},{"period":"ALL","label":"전체 틀린 단어","incorrectTermCount":340}]}"""
)
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
    @field:Schema(description = "단어 id", example = "101")
    val termId: Long,
    @field:Schema(description = "단어가 속한 단어장 id", example = "12")
    val glossaryId: Long,
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

fun WrongAnswerTermItemProjection.toResponse() = WrongAnswerTermItem(
    termId = id,
    glossaryId = glossaryId,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    totalIncorrectCount = totalIncorrectCount
)

// GlossaryEntity 가 여러 도메인 응답으로 변환되어 1:N 케이스 - 의미 키워드로 구분
fun GlossaryEntity.toWrongAnswerCard(incorrectTermCount: Long) = WrongAnswerGlossaryCard(
    glossaryId = id,
    title = title,
    category = category,
    incorrectTermCount = incorrectTermCount
)
