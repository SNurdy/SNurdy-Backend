package com.snurdy.backend.domain.dictionary.term.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "term_id_aliases")
class TermIdAliasEntity(
    @Id
    @Column(name = "alias_term_id", nullable = false)
    val aliasTermId: Long,

    @Column(name = "canonical_term_id", nullable = false)
    val canonicalTermId: Long,

    @Column(name = "source_migration", nullable = false, length = 10)
    val sourceMigration: String,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: Instant? = null
)