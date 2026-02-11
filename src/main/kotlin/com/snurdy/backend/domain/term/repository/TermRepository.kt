package com.snurdy.backend.domain.term.repository

import com.snurdy.backend.domain.term.entity.TermEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TermRepository : JpaRepository<TermEntity, Long>
