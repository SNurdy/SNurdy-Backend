package com.snurdy.backend.domain.user.streak.dto

import com.snurdy.backend.domain.user.account.entity.UserEntity
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 학습 streak 응답")
data class UserStreakResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalLearningTerms: Int
)

// UserEntity 는 auth/profile 등 여러 도메인 응답으로 변환되어 1:N 케이스 - 의미 키워드로 구분
// effectiveCurrent 는 streak 만료 판정 결과로 service 가 계산해서 전달
fun UserEntity.toStreakResponse(effectiveCurrent: Int) = UserStreakResponse(
    currentStreak = effectiveCurrent,
    longestStreak = longestStreak,
    totalLearningTerms = totalLearningTerms
)
