package com.snurdy.backend.domain.admin.auth.repository

import com.snurdy.backend.domain.admin.auth.entity.AdminUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AdminUserRepository : JpaRepository<AdminUserEntity, UUID> {
    fun findByEmail(email: String): AdminUserEntity?
}