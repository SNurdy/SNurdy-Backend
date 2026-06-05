package com.snurdy.backend.domain.progress.repository

import com.snurdy.backend.domain.progress.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GlossaryProgressRepository : JpaRepository<GlossaryProgressEntity, GlossaryProgressId> {
    fun existsByUserId(userId: UUID): Boolean

    fun countByUserId(userId: UUID): Long

    fun findByIdUserIdAndIdGlossaryId(userId: UUID, glossaryId: Long): GlossaryProgressEntity?

    fun deleteByIdUserIdAndIdGlossaryId(userId: UUID, glossaryId: Long)

    fun findAllByIdUserId(userId: UUID): List<GlossaryProgressEntity>

    fun findAllByIdUserIdAndIdGlossaryIdIn(
        userId: UUID,
        glossaryIds: Collection<Long>
    ): List<GlossaryProgressEntity>
}
