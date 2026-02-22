package com.snurdy.backend.domain.admin.changeset.repository

import com.snurdy.backend.domain.admin.changeset.entity.ChangeSetEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ChangeSetRepository : JpaRepository<ChangeSetEntity, Long>
