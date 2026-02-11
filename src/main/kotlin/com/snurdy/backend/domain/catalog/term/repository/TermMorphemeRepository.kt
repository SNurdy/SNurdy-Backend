package com.snurdy.backend.domain.catalog.term.repository

import com.snurdy.backend.domain.catalog.term.entity.TermMorphemeEntity
import com.snurdy.backend.domain.catalog.term.entity.TermMorphemeId
import org.springframework.data.jpa.repository.JpaRepository

interface TermMorphemeRepository : JpaRepository<TermMorphemeEntity, TermMorphemeId>
