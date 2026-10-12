package com.snurdy.backend.domain.user.wrongAnswerNote.dto

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.user.wrongAnswerNote.docs.WrongAnswerNoteSwaggerDescription
import com.snurdy.backend.domain.user.wrongAnswerNote.docs.WrongAnswerNoteSwaggerExample
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

@Schema(description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_PERIOD_CARD)
data class WrongAnswerPeriodCard(
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_PERIOD_CARD_PERIOD,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_PERIOD_CARD_PERIOD
    )
    val period: WrongAnswerPeriod,
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_PERIOD_CARD_LABEL,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_PERIOD_CARD_LABEL
    )
    val label: String,
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_PERIOD_CARD_INCORRECT_TERM_COUNT,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_PERIOD_CARD_INCORRECT_TERM_COUNT
    )
    val incorrectTermCount: Long
)

@Schema(
    description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_PERIOD_CARD_LIST_RESPONSE,
    example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_PERIOD_CARD_LIST_RESPONSE
)
data class WrongAnswerPeriodCardListResponse(
    val periods: List<WrongAnswerPeriodCard>
)

@Schema(description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_GLOSSARY_CARD)
data class WrongAnswerGlossaryCard(
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_GLOSSARY_CARD_GLOSSARY_ID,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_GLOSSARY_CARD_GLOSSARY_ID
    )
    val glossaryId: Long,
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_GLOSSARY_CARD_TITLE,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_GLOSSARY_CARD_TITLE
    )
    val title: String,
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_GLOSSARY_CARD_CATEGORY,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_GLOSSARY_CARD_CATEGORY
    )
    val category: String,
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_GLOSSARY_CARD_INCORRECT_TERM_COUNT,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_GLOSSARY_CARD_INCORRECT_TERM_COUNT
    )
    val incorrectTermCount: Long
)

@Schema(description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_GLOSSARY_CARD_LIST_RESPONSE)
data class WrongAnswerGlossaryCardListResponse(
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_GLOSSARY_CARD_LIST_RESPONSE_GLOSSARIES,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_GLOSSARY_CARD_LIST_RESPONSE_GLOSSARIES
    )
    val glossaries: List<WrongAnswerGlossaryCard>
)

@Schema(description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_TERM_ITEM)
data class WrongAnswerTermItem(
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_TERM_ITEM_TERM_ID,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_TERM_ITEM_TERM_ID
    )
    val termId: Long,
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_TERM_ITEM_GLOSSARY_ID,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_TERM_ITEM_GLOSSARY_ID
    )
    val glossaryId: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_TERM_ITEM_TOTAL_INCORRECT_COUNT,
        example = WrongAnswerNoteSwaggerExample.WRONG_ANSWER_TERM_ITEM_TOTAL_INCORRECT_COUNT
    )
    val totalIncorrectCount: Int
)

@Schema(description = WrongAnswerNoteSwaggerDescription.WRONG_ANSWER_TERMS_RESPONSE)
data class WrongAnswerTermsResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<WrongAnswerTermItem>
)

@Schema(description = WrongAnswerNoteSwaggerDescription.DELETE_TERMS_FROM_WRONG_ANSWER_NOTE_REQUEST)
data class DeleteTermsFromWrongAnswerNoteRequest(
    @field:NotEmpty
    @field:Size(max = 100)
    @field:Schema(
        description = WrongAnswerNoteSwaggerDescription.DELETE_TERMS_FROM_WRONG_ANSWER_NOTE_REQUEST_TERM_IDS,
        example = WrongAnswerNoteSwaggerExample.DELETE_TERMS_FROM_WRONG_ANSWER_NOTE_REQUEST_TERM_IDS
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