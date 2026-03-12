package com.snurdy.backend.domain.migration.repository

import com.snurdy.backend.domain.migration.entity.MigrationSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MigrationSessionRepository : JpaRepository<MigrationSessionEntity, UUID> {

    fun findByUserId(userId: UUID): MigrationSessionEntity?
}