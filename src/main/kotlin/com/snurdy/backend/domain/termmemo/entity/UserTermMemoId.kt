package com.snurdy.backend.domain.termmemo.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class UserTermMemoId(
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    val userId: UUID,

    @Column(name = "term_id", nullable = false)
    val termId: Long
) : Serializable
