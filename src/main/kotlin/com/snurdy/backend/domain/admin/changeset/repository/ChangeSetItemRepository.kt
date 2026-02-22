package com.snurdy.backend.domain.admin.changeset.repository

import com.snurdy.backend.domain.admin.changeset.entity.ChangeSetItemEntity
import com.snurdy.backend.domain.admin.changeset.entity.ChangeSetItemStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ChangeSetItemRepository : JpaRepository<ChangeSetItemEntity, Long> {
    fun findByChangeSetIdOrderBySortOrderAscIdAsc(changeSetId: Long): List<ChangeSetItemEntity>
    fun countByChangeSetId(changeSetId: Long): Long
    fun countByChangeSetIdAndStatus(changeSetId: Long, status: ChangeSetItemStatus): Long
    fun findTopByChangeSetIdOrderBySortOrderDescIdDesc(changeSetId: Long): ChangeSetItemEntity?
    fun findByIdAndChangeSetId(id: Long, changeSetId: Long): ChangeSetItemEntity?
}
