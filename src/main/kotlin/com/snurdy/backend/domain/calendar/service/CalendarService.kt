package com.snurdy.backend.domain.calendar.service

import com.snurdy.backend.domain.calendar.dto.CalendarResponse
import com.snurdy.backend.domain.calendar.dto.toResponse
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
class CalendarService(
    private val dailyProgressRepository: DailyProgressRepository
) {

    @Transactional(readOnly = true)
    fun getMonthly(userId: UUID, year: Int, month: Int): CalendarResponse {
        val yearMonth = YearMonth.of(year, month)
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()

        val days = dailyProgressRepository.findByIdUserIdAndIdDateBetweenOrderByIdDateAsc(userId, from, to)
            .map { it.toResponse() }

        return CalendarResponse(
            year = year,
            month = month,
            days = days,
            totalLearnedCount = days.sumOf { it.learnedCount },
            totalCompletedCount = days.sumOf { it.completedCount }
        )
    }
}
