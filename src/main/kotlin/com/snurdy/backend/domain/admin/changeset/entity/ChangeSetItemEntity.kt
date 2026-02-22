package com.snurdy.backend.domain.admin.changeset.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.*

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

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 20)
    var operation: ChangeSetOperation,

    @Column(name = "before_json", columnDefinition = "JSON")
    var beforeJson: String? = null,

    @Column(name = "after_json", columnDefinition = "JSON")
    var afterJson: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ChangeSetItemStatus = ChangeSetItemStatus.PENDING,

    @Column(name = "error_code", length = 100)
    var errorCode: String? = null,

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
) : BaseTimeEntity()

enum class ChangeSetOperation {
    CREATE,
    UPDATE,
    DELETE,
    LINK,
    UNLINK
}

enum class ChangeSetItemStatus {
    PENDING,
    VALIDATED,
    APPLIED,
    FAILED,
    SKIPPED
}
