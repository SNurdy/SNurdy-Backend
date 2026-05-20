package com.snurdy.backend.domain.calendar.dto

import java.time.LocalDate

data class CalendarResponse(
    val year: Int,
    val month: Int,
    val days: List<CalendarDailyItem>,
    val totalLearnedCount: Int,
    val totalReviewedCount: Int
)

data class CalendarDailyItem(
    val date: LocalDate,
    val learnedCount: Int,
    val reviewedCount: Int
)
