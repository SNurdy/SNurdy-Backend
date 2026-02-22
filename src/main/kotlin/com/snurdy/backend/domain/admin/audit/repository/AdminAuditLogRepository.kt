package com.snurdy.backend.domain.admin.audit.repository

import com.snurdy.backend.domain.admin.audit.entity.AdminAuditLogEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AdminAuditLogRepository : JpaRepository<AdminAuditLogEntity, Long>
