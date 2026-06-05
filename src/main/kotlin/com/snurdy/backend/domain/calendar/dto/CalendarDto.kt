package com.snurdy.backend.domain.calendar.dto

import com.snurdy.backend.domain.progress.entity.DailyProgressEntity
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "월별 학습 캘린더 응답")
data class CalendarResponse(
    val year: Int,
    val month: Int,
    val days: List<CalendarDailyItem>,
    val totalLearnedCount: Int,
    val totalCompletedCount: Int
)

@Schema(description = "캘린더 일자별 학습 항목")
data class CalendarDailyItem(
    val date: LocalDate,
    @field:Schema(description = "그 날 처음 학습된 단어 수 (NEW -> LEARNING 전환)")
    val learnedCount: Int,
    @field:Schema(description = "그 날 처리된 학습 이벤트 총 개수 (신규 + 복습)")
    val completedCount: Int,
    @field:Schema(description = "그 날 학습 이벤트가 1건이라도 있었는지 여부")
    val studied: Boolean
)

fun DailyProgressEntity.toResponse() = CalendarDailyItem(
    date = id.date,
    learnedCount = learnedCount,
    completedCount = completedCount,
    studied = completedCount > 0
)
