package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.learning.entity.GlossaryProgressId
import org.springframework.data.jpa.repository.JpaRepository

interface GlossaryProgressRepository : JpaRepository<GlossaryProgressEntity, GlossaryProgressId>