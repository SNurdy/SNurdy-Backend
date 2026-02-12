package com.snurdy.backend.domain.catalog.glossary.repository

import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GlossaryRepository : JpaRepository<GlossaryEntity, Long>
