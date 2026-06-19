package com.snurdy.backend.domain.progress.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.time.LocalDate
import java.util.UUID

@Embeddable
data class DailyProgressId(
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    val userId: UUID,

    @Column(name = "date", nullable = false)
    val date: LocalDate
) : Serializable
