package com.snurdy.backend.domain.version.repository

import com.snurdy.backend.domain.version.entity.AppVersionEntity
import com.snurdy.backend.domain.version.entity.Platform
import org.springframework.data.jpa.repository.JpaRepository

interface AppVersionRepository : JpaRepository<AppVersionEntity, Long> {
    fun findByPlatform(platform: Platform): AppVersionEntity?
}