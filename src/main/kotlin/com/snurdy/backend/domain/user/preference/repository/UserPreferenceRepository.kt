package com.snurdy.backend.domain.user.preference.repository

import com.snurdy.backend.domain.user.preference.entity.UserPreferenceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserPreferenceRepository : JpaRepository<UserPreferenceEntity, UUID>
