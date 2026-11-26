package com.snurdy.backend.domain.dictionary.term.repository

import com.snurdy.backend.domain.dictionary.term.entity.TermIdAliasEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TermIdAliasRepository : JpaRepository<TermIdAliasEntity, Long> {

    fun findByAliasTermIdIn(aliasTermIds: Collection<Long>): List<TermIdAliasEntity>
}