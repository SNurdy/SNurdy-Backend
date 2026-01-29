package com.snurdy.backend.domain.auth.repository

import com.snurdy.backend.domain.auth.entity.RefreshTokenEntity
import org.springframework.data.repository.CrudRepository
import java.util.*

interface RefreshTokenRepository : CrudRepository<RefreshTokenEntity, UUID> {
    fun findByToken(token: String): RefreshTokenEntity?
    fun deleteByUserId(userId: UUID)
    fun findByUserId(userId: UUID): RefreshTokenEntity?
}