package com.snurdy.backend.domain.catalog.glossary.repository

import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.catalog.glossary.entity.GlossaryTermId
import org.springframework.data.jpa.repository.JpaRepository

interface GlossaryTermRepository : JpaRepository<GlossaryTermEntity, GlossaryTermId>
