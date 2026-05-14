package com.snurdy.backend.domain.admin.audit.repository

import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AdminAuditLogRepository : JpaRepository<AdminAuditLogEntity, Long> {

    fun findByTargetTableAndTargetPkOrderByIdDesc(
        targetTable: String,
        targetPk: String,
        pageable: Pageable
    ): Page<AdminAuditLogEntity>

    fun findByTargetTableOrderByIdDesc(
        targetTable: String,
        pageable: Pageable
    ): Page<AdminAuditLogEntity>

    fun findByActionOrderByIdDesc(
        action: String,
        pageable: Pageable
    ): Page<AdminAuditLogEntity>

    fun findAllByOrderByIdDesc(pageable: Pageable): Page<AdminAuditLogEntity>
}