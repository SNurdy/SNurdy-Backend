package com.snurdy.backend.domain.admin.changeset.repository

import com.snurdy.backend.domain.admin.changeset.entity.ChangeSetItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ChangeSetItemRepository : JpaRepository<ChangeSetItemEntity, Long> {
    fun findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId: Long): List<ChangeSetItemEntity>
    fun countByChangeSetId(changeSetId: Long): Long
}
