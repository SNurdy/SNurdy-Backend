package com.snurdy.backend.domain.progress.service

import com.snurdy.backend.domain.progress.dto.ProgressEvent
import com.snurdy.backend.domain.progress.entity.TermProgressEntity
import com.snurdy.backend.domain.progress.entity.TermProgressStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

// SM-2 변형 간격 반복 학습(SRS) 알고리즘. DB 조회 없는 순수 계산 컴포넌트
object SrsCalculator {

    val DEFAULT_EASE_FACTOR: BigDecimal = BigDecimal("2.50")
    val MIN_EASE_FACTOR: BigDecimal = BigDecimal("1.30")
    private val EASE_DECREASE: BigDecimal = BigDecimal("0.20")

    // return = 단어가 이번 호출로 처음 출제됐는지 (정답/오답 무관)
    // iOS 합의 — 출제만 됐으면 learnedCount += 1. 같은 단어가 두 번째 이후 재출제될 땐 카운트 안 함
    fun applyAnswer(progress: TermProgressEntity, event: ProgressEvent, now: Instant): Boolean {
        val firstAppearance = progress.lastReviewedAt == null

        if (event.correct) {
            val previousReviewCount = progress.reviewCount
            val nextReviewCount = previousReviewCount + 1
            val nextInterval = calculateInterval(progress.intervalDays, progress.easeFactor, nextReviewCount)

            progress.reviewCount = nextReviewCount
            progress.intervalDays = nextInterval
            progress.status = if (nextReviewCount >= 3) TermProgressStatus.REVIEW else TermProgressStatus.LEARNING
            progress.lastReviewedAt = now
            progress.nextReviewAt = now.plus(nextInterval.toLong(), ChronoUnit.DAYS)
            return firstAppearance
        }

        progress.easeFactor = progress.easeFactor
            .subtract(EASE_DECREASE)
            .max(MIN_EASE_FACTOR)
            .setScale(2, RoundingMode.HALF_UP)
        progress.intervalDays = 1
        progress.reviewCount = 0
        progress.incorrectCount += 1
        progress.status = TermProgressStatus.LEARNING
        progress.lastReviewedAt = now
        progress.nextReviewAt = now.plus(1, ChronoUnit.DAYS)
        return firstAppearance
    }

    private fun calculateInterval(currentInterval: Int, easeFactor: BigDecimal, reviewCount: Int): Int =
        when (reviewCount) {
            1 -> 1
            2 -> 3
            else -> ceil(currentInterval.coerceAtLeast(1) * easeFactor.toDouble()).toInt().coerceAtLeast(1)
        }
}
