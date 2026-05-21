package com.snurdy.backend.domain.user.streak.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 학습 streak 응답")
data class UserStreakResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalLearningTerms: Int
)
