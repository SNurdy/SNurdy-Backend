package com.snurdy.backend.domain.migration.repository

import com.snurdy.backend.domain.migration.entity.MigrationSessionEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MigrationSessionRepository : JpaRepository<MigrationSessionEntity, UUID> {

    fun findByUserId(userId: UUID): MigrationSessionEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ms from MigrationSessionEntity ms where ms.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): MigrationSessionEntity?
}
}