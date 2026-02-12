package com.snurdy.backend.domain.catalog.term.repository

import com.snurdy.backend.domain.catalog.term.entity.TermEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TermRepository : JpaRepository<TermEntity, Long>
