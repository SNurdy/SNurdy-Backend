package com.snurdy.backend.domain.admin.workflow.repository

import com.snurdy.backend.domain.admin.workflow.entity.ChangeSetItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ChangeSetItemRepository : JpaRepository<ChangeSetItemEntity, Long> {
    fun findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId: Long): List<ChangeSetItemEntity>
}
