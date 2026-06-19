package com.snurdy.backend.domain.admin.workflow.entity

import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.exception.ApiException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "change_requests")
@EntityListeners(AuditingEntityListener::class)
class ChangeRequestEntity(
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
    var status: ChangeRequestStatus = ChangeRequestStatus.REQUESTED,

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: ChangeRequestEntityType,

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 20)
    var operation: ChangeRequestOperation,

    @Column(name = "entity_pk", nullable = false, length = 100)
    var entityPk: String = NEW_ENTITY_PK,

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

    @Column(name = "applied_at")
    var appliedAt: Instant? = null,

    @Column(name = "rolled_back_at")
    var rolledBackAt: Instant? = null
) {

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
        protected set

    fun markValidated(warning: String? = null) {
        this.status = ChangeRequestStatus.VALIDATED
        this.errorCode = null
        this.errorMessage = null
        this.warningMessage = warning
    }

    fun markFailed(errorCode: String, errorMessage: String) {
        this.status = ChangeRequestStatus.FAILED
        this.errorCode = errorCode
        this.errorMessage = errorMessage
        this.warningMessage = null
    }

    fun markApplied(resolvedEntityPk: String? = null) {
        this.status = ChangeRequestStatus.APPLIED
        this.appliedAt = Instant.now()
        if (resolvedEntityPk != null) {
            this.entityPk = resolvedEntityPk
        }
    }

    fun markCanceled() {
        this.status = ChangeRequestStatus.CANCELED
    }

    fun markRolledBack() {
        this.status = ChangeRequestStatus.ROLLED_BACK
        this.rolledBackAt = Instant.now()
    }

    fun resetForRetry() {
        this.status = ChangeRequestStatus.REQUESTED
        this.errorCode = null
        this.errorMessage = null
        this.warningMessage = null
    }

    fun isEditable(): Boolean =
        status == ChangeRequestStatus.REQUESTED || status == ChangeRequestStatus.FAILED

    fun isValidatable(): Boolean =
        status == ChangeRequestStatus.REQUESTED

    fun isApplicable(): Boolean =
        status == ChangeRequestStatus.VALIDATED

    fun isCancelable(): Boolean =
        status == ChangeRequestStatus.REQUESTED ||
                status == ChangeRequestStatus.FAILED ||
                status == ChangeRequestStatus.VALIDATED

    fun isRollbackable(): Boolean =
        status == ChangeRequestStatus.APPLIED

    fun requireId(): Long = id ?: throw ApiException(AdminError.CHANGE_REQUEST_INVALID_STATE)

    companion object {
        const val NEW_ENTITY_PK = "NEW"
    }
}
