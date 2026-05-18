package com.snurdy.backend.domain.migration.entity

import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "migration_sessions")
class MigrationSessionEntity(

    @Id
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: MigrationStatus = MigrationStatus.IN_PROGRESS,

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null,

    @Column(name = "completed_at")
    var completedAt: Instant? = null
) : BaseTimeEntity() {
    fun complete() {
        this.status = MigrationStatus.COMPLETED
        this.errorMessage = null
        this.completedAt = Instant.now()
    }

    fun fail(reason: String) {
        this.status = MigrationStatus.FAILED
        this.errorMessage = reason.take(MAX_ERROR_MESSAGE_LENGTH)
    }

    fun retry() {
        this.status = MigrationStatus.IN_PROGRESS
        this.errorMessage = null
    }

    fun markUploaded() {
        this.status = MigrationStatus.UPLOADED
        this.errorMessage = null
    }

    fun isUploaded() = status == MigrationStatus.UPLOADED
    fun isCompleted() = status == MigrationStatus.COMPLETED
    fun isInProgress() = status == MigrationStatus.IN_PROGRESS
    fun isFailed() = status == MigrationStatus.FAILED

    companion object {
        private const val MAX_ERROR_MESSAGE_LENGTH = 500
    }
}
