package com.snurdy.backend.domain.morpheme.repository

import com.snurdy.backend.domain.morpheme.entity.MorphemeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MorphemeRepository : JpaRepository<MorphemeEntity, Long>
