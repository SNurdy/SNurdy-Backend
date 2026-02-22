package com.snurdy.backend.domain.admin.workflow.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "change_sets")
class ChangeSetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "title", nullable = false, length = 150)
    var title: String,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @Column(name = "status", nullable = false, length = 30)
    var status: String,

    @Column(name = "source_env", nullable = false, length = 20)
    var sourceEnv: String = "DEV",

    @Column(name = "target_env", nullable = false, length = 20)
    var targetEnv: String = "DEV",

    @Column(name = "requested_by", nullable = false, columnDefinition = "BINARY(16)")
    var requestedBy: UUID,

    @Column(name = "approved_by", columnDefinition = "BINARY(16)")
    var approvedBy: UUID? = null,

    @Column(name = "approved_at")
    var approvedAt: Instant? = null,

    @Column(name = "applied_by", columnDefinition = "BINARY(16)")
    var appliedBy: UUID? = null,

    @Column(name = "applied_at")
    var appliedAt: Instant? = null,

    @Column(name = "validation_summary", columnDefinition = "JSON")
    var validationSummary: String? = null,

    @Column(name = "apply_summary", columnDefinition = "JSON")
    var applySummary: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
