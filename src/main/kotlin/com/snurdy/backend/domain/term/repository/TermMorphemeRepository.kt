package com.snurdy.backend.domain.term.repository

import com.snurdy.backend.domain.term.entity.TermMorphemeEntity
import com.snurdy.backend.domain.term.entity.TermMorphemeId
import org.springframework.data.jpa.repository.JpaRepository

interface TermMorphemeRepository : JpaRepository<TermMorphemeEntity, TermMorphemeId>
