package com.snurdy.backend.domain.admin.audit.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "admin_audit_logs")
class AdminAuditLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L,

    @Column(name = "actor_role", nullable = false, length = 30)
    var actorRole: String,

    @Column(name = "action", nullable = false, length = 30)
    var action: String,

    @Column(name = "target_table", nullable = false, length = 50)
    var targetTable: String,

    @Column(name = "target_pk", nullable = false, length = 100)
    var targetPk: String,

    @Column(name = "change_set_id")
    var changeSetId: Long? = null,

    @Column(name = "request_id", length = 64)
    var requestId: String? = null,

    @Column(name = "before_json", columnDefinition = "JSON")
    var beforeJson: String? = null,

    @Column(name = "after_json", columnDefinition = "JSON")
    var afterJson: String? = null,

    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null,

    @Column(name = "user_agent", length = 255)
    var userAgent: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
