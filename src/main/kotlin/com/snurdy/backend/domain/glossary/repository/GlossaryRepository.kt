package com.snurdy.backend.domain.glossary.repository

import com.snurdy.backend.domain.glossary.entity.GlossaryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GlossaryRepository : JpaRepository<GlossaryEntity, Long>
