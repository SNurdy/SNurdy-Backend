package com.snurdy.backend.domain.admin.workflow.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "change_set_items")
class ChangeSetItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "change_set_id", nullable = false)
    var changeSetId: Long,

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String,

    @Column(name = "entity_pk", nullable = false, length = 100)
    var entityPk: String,

    @Column(name = "operation", nullable = false, length = 20)
    var operation: String,

    @Column(name = "before_json", columnDefinition = "JSON")
    var beforeJson: String? = null,

    @Column(name = "after_json", columnDefinition = "JSON")
    var afterJson: String? = null,

    @Column(name = "status", nullable = false, length = 30)
    var status: String,

    @Column(name = "error_code", length = 100)
    var errorCode: String? = null,

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
