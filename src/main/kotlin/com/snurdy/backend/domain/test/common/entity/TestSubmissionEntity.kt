package com.snurdy.backend.domain.test.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "test_submissions")
class TestSubmissionEntity(
    @EmbeddedId
    val id: TestSubmissionId,

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 40)
    val submissionType: TestSubmissionType,

    @Column(name = "response_body", columnDefinition = "JSON", nullable = false)
    val responseBody: String,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: Instant? = null
)