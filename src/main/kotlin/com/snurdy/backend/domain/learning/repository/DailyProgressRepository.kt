package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.DailyProgressEntity
import com.snurdy.backend.domain.learning.entity.DailyProgressId
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface DailyProgressRepository : JpaRepository<DailyProgressEntity, DailyProgressId> {
    fun existsByIdUserId(userId: UUID): Boolean

    fun countByIdUserId(userId: UUID): Long

    fun findByIdUserIdAndIdDateBetweenOrderByIdDateAsc(
        userId: UUID,
        from: LocalDate,
        to: LocalDate
    ): List<DailyProgressEntity>

    fun findByIdUserIdOrderByIdDateDesc(userId: UUID): List<DailyProgressEntity>

    fun findAllByIdUserIdAndIdDateIn(
        userId: UUID,
        dates: Collection<LocalDate>
    ): List<DailyProgressEntity>

    /** [from] 포함 이하 날짜 범위에서 내림차순으로 조회. streak 역산(cap 기반) 용도. */
    fun findByIdUserIdAndIdDateLessThanEqualOrderByIdDateDesc(
        userId: UUID,
        date: LocalDate,
        pageable: org.springframework.data.domain.Pageable
    ): List<DailyProgressEntity>
}
