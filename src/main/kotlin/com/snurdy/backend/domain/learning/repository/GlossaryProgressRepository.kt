package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.GlossaryLearningStateEntity
import com.snurdy.backend.domain.learning.entity.GlossaryLearningStateId
import org.springframework.data.jpa.repository.JpaRepository

interface GlossaryProgressRepository : JpaRepository<GlossaryLearningStateEntity, GlossaryLearningStateId>
