package com.snurdy.backend.domain.catalog.morpheme.repository

import com.snurdy.backend.domain.catalog.morpheme.entity.MorphemeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MorphemeRepository : JpaRepository<MorphemeEntity, Long>
