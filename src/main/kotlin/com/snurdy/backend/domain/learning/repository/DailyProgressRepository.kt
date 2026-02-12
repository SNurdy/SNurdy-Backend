package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.DailyProgressEntity
import com.snurdy.backend.domain.learning.entity.DailyProgressId
import org.springframework.data.jpa.repository.JpaRepository

interface DailyProgressRepository : JpaRepository<DailyProgressEntity, DailyProgressId>
