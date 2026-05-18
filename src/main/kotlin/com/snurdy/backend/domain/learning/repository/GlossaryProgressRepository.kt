package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.learning.entity.GlossaryProgressId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GlossaryProgressRepository : JpaRepository<GlossaryProgressEntity, GlossaryProgressId> {
    fun existsByUserId(userId: UUID): Boolean

    fun countByUserId(userId: UUID): Long

    fun findByIdUserIdAndIdGlossaryId(userId: UUID, glossaryId: Long): GlossaryProgressEntity?

    fun findAllByIdUserId(userId: UUID): List<GlossaryProgressEntity>

    fun findAllByIdUserIdAndIdGlossaryIdIn(
        userId: UUID,
        glossaryIds: Collection<Long>
    ): List<GlossaryProgressEntity>
}
