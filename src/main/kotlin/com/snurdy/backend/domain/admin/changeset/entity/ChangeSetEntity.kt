package com.snurdy.backend.domain.admin.changeset.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.*
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ChangeSetStatus = ChangeSetStatus.DRAFT,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_env", nullable = false, length = 20)
    var sourceEnv: ChangeSetEnvironment = ChangeSetEnvironment.DEV,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_env", nullable = false, length = 20)
    var targetEnv: ChangeSetEnvironment = ChangeSetEnvironment.PROD,

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
    var applySummary: String? = null
) : BaseTimeEntity()

enum class ChangeSetStatus {
    DRAFT,
    VALIDATED,
    APPROVED,
    APPLIED_DEV,
    APPLIED_PROD,
    FAILED,
    CANCELED
}

enum class ChangeSetEnvironment {
    DEV,
    PROD
}
