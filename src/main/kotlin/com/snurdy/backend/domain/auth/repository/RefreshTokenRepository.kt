package com.snurdy.backend.domain.auth.repository

import com.snurdy.backend.domain.auth.entity.RefreshToken
import org.springframework.data.repository.CrudRepository
import java.util.*

interface RefreshTokenRepository : CrudRepository<RefreshToken, UUID> {
    fun findByToken(token: String): RefreshToken?
    fun deleteByUserId(userId: UUID)
}