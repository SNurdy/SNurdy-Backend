package com.snurdy.backend.domain.user.customNote.repository

import com.snurdy.backend.domain.user.customNote.entity.CustomNoteShareCodeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CustomNoteShareCodeRepository : JpaRepository<CustomNoteShareCodeEntity, Long> {

    fun findByShareCodeAndRevokedAtIsNull(shareCode: String): CustomNoteShareCodeEntity?

    fun existsByShareCode(shareCode: String): Boolean
}
