package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.DailyProgressEntity
import com.snurdy.backend.domain.learning.entity.DailyProgressId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DailyProgressRepository : JpaRepository<DailyProgressEntity, DailyProgressId> {
    fun existsByIdUserId(userId: UUID): Boolean

    fun countByIdUserId(userId: UUID): Long
}
