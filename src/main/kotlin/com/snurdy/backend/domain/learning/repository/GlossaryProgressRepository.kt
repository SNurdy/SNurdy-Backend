package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.learning.entity.GlossaryProgressId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GlossaryProgressRepository : JpaRepository<GlossaryProgressEntity, GlossaryProgressId> {
    fun existsByUserId(userId: UUID): Boolean
}