package com.snurdy.backend.domain.admin.workflow.repository

import com.snurdy.backend.domain.admin.workflow.entity.ChangeSetEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChangeSetRepository : JpaRepository<ChangeSetEntity, Long> {
    fun findByStatusOrderByIdDesc(status: String, pageable: Pageable): Page<ChangeSetEntity>
}
