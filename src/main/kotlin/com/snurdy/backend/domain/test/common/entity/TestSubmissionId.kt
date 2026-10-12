package com.snurdy.backend.domain.test.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class TestSubmissionId(
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "idempotency_key", columnDefinition = "BINARY(16)")
    val idempotencyKey: UUID = UUID.randomUUID()
) : Serializable