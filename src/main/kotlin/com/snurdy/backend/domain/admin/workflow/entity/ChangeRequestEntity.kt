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
@Table(name = "change_requests")
class ChangeRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "title", nullable = false, length = 150)
    var title: String,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @Column(name = "status", nullable = false, length = 30)
    var status: String = ChangeRequestStatus.REQUESTED,

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String,

    @Column(name = "operation", nullable = false, length = 20)
    var operation: String,

    @Column(name = "entity_pk", nullable = false, length = 100)
    var entityPk: String = "NEW",

    @Column(name = "before_json", columnDefinition = "JSON")
    var beforeJson: String? = null,

    @Column(name = "after_json", columnDefinition = "JSON")
    var afterJson: String? = null,

    @Column(name = "error_code", length = 100)
    var errorCode: String? = null,

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null,

    @Column(name = "warning_message", length = 1000)
    var warningMessage: String? = null,

    @Column(name = "requested_by", nullable = false, columnDefinition = "BINARY(16)")
    var requestedBy: UUID,

    @Column(name = "applied_by", columnDefinition = "BINARY(16)")
    var appliedBy: UUID? = null,

    @Column(name = "applied_at")
    var appliedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {

    fun markValidated(warning: String? = null) {
        this.status = ChangeRequestStatus.VALIDATED
        this.errorCode = null
        this.errorMessage = null
        this.warningMessage = warning
        this.updatedAt = Instant.now()
    }

    fun markFailed(errorCode: String, errorMessage: String) {
        this.status = ChangeRequestStatus.FAILED
        this.errorCode = errorCode
        this.errorMessage = errorMessage
        this.warningMessage = null
        this.updatedAt = Instant.now()
    }

    fun markApplied(actorUserId: UUID, resolvedEntityPk: String? = null) {
        val now = Instant.now()
        this.status = ChangeRequestStatus.APPLIED
        this.appliedBy = actorUserId
        this.appliedAt = now
        this.updatedAt = now
        if (resolvedEntityPk != null) {
            this.entityPk = resolvedEntityPk
        }
    }

    fun markCanceled() {
        this.status = ChangeRequestStatus.CANCELED
        this.updatedAt = Instant.now()
    }

    fun resetForRetry() {
        this.status = ChangeRequestStatus.REQUESTED
        this.errorCode = null
        this.errorMessage = null
        this.warningMessage = null
        this.updatedAt = Instant.now()
    }

    fun isEditable(): Boolean =
        status in setOf(ChangeRequestStatus.REQUESTED, ChangeRequestStatus.FAILED)

    fun isValidatable(): Boolean =
        status == ChangeRequestStatus.REQUESTED

    fun isApplicable(): Boolean =
        status == ChangeRequestStatus.VALIDATED

    fun isCancelable(): Boolean =
        status in setOf(
            ChangeRequestStatus.REQUESTED,
            ChangeRequestStatus.FAILED,
            ChangeRequestStatus.VALIDATED
        )
}