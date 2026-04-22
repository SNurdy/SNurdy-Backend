package com.snurdy.backend.domain.admin.workflow.repository

import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestEntity
import com.snurdy.backend.domain.admin.workflow.entity.ChangeRequestStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ChangeRequestRepository : JpaRepository<ChangeRequestEntity, Long> {

    fun findByStatusOrderByIdDesc(status: ChangeRequestStatus, pageable: Pageable): Page<ChangeRequestEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChangeRequestEntity c WHERE c.id = :id")
    fun findByIdForUpdate(id: Long): ChangeRequestEntity?
}
