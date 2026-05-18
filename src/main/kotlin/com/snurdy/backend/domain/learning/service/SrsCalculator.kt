package com.snurdy.backend.domain.learning.service

import com.snurdy.backend.domain.learning.dto.LearningAnswerEvent
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * SM-2 변형 간격 반복 학습(SRS) 알고리즘.
 * DB 를 조회하지 않는 순수 계산 컴포넌트라 단위 테스트 시 Mock 이 필요 없다.
 */
object SrsCalculator {

    val DEFAULT_EASE_FACTOR: BigDecimal = BigDecimal("2.50")
    val MIN_EASE_FACTOR: BigDecimal = BigDecimal("1.30")
    private val EASE_DECREASE: BigDecimal = BigDecimal("0.20")

    fun applyAnswer(progress: TermProgressEntity, event: LearningAnswerEvent, now: Instant): Boolean {
        if (event.correct) {
            val previousReviewCount = progress.reviewCount
            val nextReviewCount = previousReviewCount + 1
            val nextInterval = calculateInterval(progress.intervalDays, progress.easeFactor, nextReviewCount)

            progress.reviewCount = nextReviewCount
            progress.intervalDays = nextInterval
            progress.status = if (nextReviewCount >= 3) TermProgressStatus.REVIEW else TermProgressStatus.LEARNING
            progress.lastReviewedAt = now
            progress.nextReviewAt = now.plus(nextInterval.toLong(), ChronoUnit.DAYS)

            return previousReviewCount == 0
        }

        progress.easeFactor = progress.easeFactor
            .subtract(EASE_DECREASE)
            .max(MIN_EASE_FACTOR)
            .setScale(2, RoundingMode.HALF_UP)
        progress.intervalDays = 1
        progress.reviewCount = 0
        progress.status = TermProgressStatus.LEARNING
        progress.lastReviewedAt = now
        progress.nextReviewAt = now.plus(1, ChronoUnit.DAYS)
        return false
    }

    private fun calculateInterval(currentInterval: Int, easeFactor: BigDecimal, reviewCount: Int): Int =
        when (reviewCount) {
            1 -> 1
            2 -> 3
            else -> ceil(currentInterval.coerceAtLeast(1) * easeFactor.toDouble()).toInt().coerceAtLeast(1)
        }
}
