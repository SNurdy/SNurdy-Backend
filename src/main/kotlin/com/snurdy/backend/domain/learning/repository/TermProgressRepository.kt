package com.snurdy.backend.domain.learning.repository

import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TermProgressRepository : JpaRepository<TermProgressEntity, UUID>
