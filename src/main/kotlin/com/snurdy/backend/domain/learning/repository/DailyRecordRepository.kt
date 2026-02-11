package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.DailyLearningEntity
import com.snurdy.backend.domain.learning.entity.DailyLearningId
import org.springframework.data.jpa.repository.JpaRepository

interface DailyRecordRepository : JpaRepository<DailyLearningEntity, DailyLearningId>
