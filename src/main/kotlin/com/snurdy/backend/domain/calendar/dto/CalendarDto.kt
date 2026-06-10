package com.snurdy.backend.domain.calendar.dto

import com.snurdy.backend.domain.progress.entity.DailyProgressEntity
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "월별 학습 캘린더 응답")
data class CalendarResponse(
    val year: Int,
    val month: Int,
    val days: List<CalendarDailyItem>,
    @field:Schema(description = "이달 신규 학습 단어 합")
    val totalNewLearnedCount: Int,
    @field:Schema(description = "이달 전체 학습 단어 합")
    val totalAllLearnedCount: Int
)

@Schema(description = "캘린더 일자별 학습 항목")
data class CalendarDailyItem(
    val date: LocalDate,
    @field:Schema(description = "그 날 신규로 학습한 단어 수. NEW 에서 LEARNING 으로 전환")
    val newLearnedCount: Int,
    @field:Schema(description = "그 날 전체 학습한 단어 수. 신규 + 복습 합")
    val allLearnedCount: Int,
    @field:Schema(description = "그 날 학습 이벤트가 1건이라도 있었는지 여부")
    val studied: Boolean
)

fun DailyProgressEntity.toResponse() = CalendarDailyItem(
    date = id.date,
    newLearnedCount = newLearnedCount,
    allLearnedCount = allLearnedCount,
    studied = allLearnedCount > 0
)
