package com.snurdy.backend.domain.glossary.repository

import com.snurdy.backend.domain.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.glossary.entity.GlossaryTermId
import org.springframework.data.jpa.repository.JpaRepository

interface GlossaryTermRepository : JpaRepository<GlossaryTermEntity, GlossaryTermId>
