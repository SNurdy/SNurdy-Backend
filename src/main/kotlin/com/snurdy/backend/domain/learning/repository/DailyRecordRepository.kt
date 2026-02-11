package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.DailyRecordEntity
import com.snurdy.backend.domain.learning.entity.DailyRecordId
import org.springframework.data.jpa.repository.JpaRepository

interface DailyRecordRepository : JpaRepository<DailyRecordEntity, DailyRecordId>
