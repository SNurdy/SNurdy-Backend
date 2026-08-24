package com.snurdy.backend.domain.progress.service

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.progress.dto.ProgressEvent
import com.snurdy.backend.domain.progress.entity.TermProgressEntity
import com.snurdy.backend.domain.progress.entity.TermProgressStatus
import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class SrsCalculatorTest {

    @Test
    fun `첫 출제 정답 시 reviewCount는 1 intervalDays는 1 status는 LEARNING이고 firstAppearance를 반환한다`() {
        // Given
        val now = Instant.parse("2025-01-01T10:00:00Z")
        val progress = createProgress(reviewCount = 0, intervalDays = 0, lastReviewedAt = null)

        // When
        val firstAppearance = SrsCalculator.applyAnswer(progress, correctEvent(), now)

        // Then
        assertTrue(firstAppearance)
        assertEquals(1, progress.reviewCount)
        assertEquals(1, progress.intervalDays)
        assertEquals(TermProgressStatus.LEARNING, progress.status)
        assertEquals(now, progress.lastReviewedAt)
        assertEquals(now.plus(1, ChronoUnit.DAYS), progress.nextReviewAt)
    }

    @Test
    fun `두 번째 출제 정답 시 intervalDays는 3 status는 LEARNING이고 firstAppearance가 아니다`() {
        // Given
        val now = Instant.parse("2025-01-02T10:00:00Z")
        val progress = createProgress(
            reviewCount = 1,
            intervalDays = 1,
            lastReviewedAt = Instant.parse("2025-01-01T10:00:00Z")
        )

        // When
        val firstAppearance = SrsCalculator.applyAnswer(progress, correctEvent(), now)

        // Then
        assertFalse(firstAppearance)
        assertEquals(2, progress.reviewCount)
        assertEquals(3, progress.intervalDays)
        assertEquals(TermProgressStatus.LEARNING, progress.status)
        assertEquals(now.plus(3, ChronoUnit.DAYS), progress.nextReviewAt)
    }

    @Test
    fun `세 번째 출제 정답 시 status는 REVIEW로 전환되고 intervalDays는 currentInterval과 easeFactor 곱의 ceiling이다`() {
        // Given
        val now = Instant.parse("2025-01-05T10:00:00Z")
        val progress = createProgress(
            reviewCount = 2,
            intervalDays = 3,
            easeFactor = BigDecimal("2.50"),
            lastReviewedAt = Instant.parse("2025-01-02T10:00:00Z")
        )

        // When
        SrsCalculator.applyAnswer(progress, correctEvent(), now)

        // Then
        assertEquals(3, progress.reviewCount)
        assertEquals(8, progress.intervalDays)
        assertEquals(TermProgressStatus.REVIEW, progress.status)
        assertEquals(now.plus(8, ChronoUnit.DAYS), progress.nextReviewAt)
    }

    @Test
    fun `REVIEW 단계에서 추가 정답 시 intervalDays는 easeFactor만큼 곱해져 누적된다`() {
        // Given
        val now = Instant.parse("2025-02-01T10:00:00Z")
        val progress = createProgress(
            reviewCount = 3,
            intervalDays = 8,
            easeFactor = BigDecimal("2.50"),
            lastReviewedAt = Instant.parse("2025-01-24T10:00:00Z"),
            status = TermProgressStatus.REVIEW
        )

        // When
        SrsCalculator.applyAnswer(progress, correctEvent(), now)

        // Then
        assertEquals(4, progress.reviewCount)
        assertEquals(20, progress.intervalDays)
        assertEquals(TermProgressStatus.REVIEW, progress.status)
    }

    @Test
    fun `정답 시 easeFactor는 변하지 않는다`() {
        // Given
        val now = Instant.parse("2025-01-05T10:00:00Z")
        val originalEaseFactor = BigDecimal("2.30")
        val progress = createProgress(
            reviewCount = 2,
            intervalDays = 3,
            easeFactor = originalEaseFactor,
            lastReviewedAt = Instant.parse("2025-01-02T10:00:00Z")
        )

        // When
        SrsCalculator.applyAnswer(progress, correctEvent(), now)

        // Then
        assertEquals(originalEaseFactor, progress.easeFactor)
    }

    @Test
    fun `진행 중 단어 오답 시 easeFactor는 0_20 감소하고 reviewCount는 0 intervalDays는 1 incorrectCount는 1 증가한다`() {
        // Given
        val now = Instant.parse("2025-01-05T10:00:00Z")
        val progress = createProgress(
            reviewCount = 2,
            intervalDays = 3,
            easeFactor = BigDecimal("2.50"),
            incorrectCount = 0,
            lastReviewedAt = Instant.parse("2025-01-02T10:00:00Z")
        )

        // When
        val firstAppearance = SrsCalculator.applyAnswer(progress, incorrectEvent(), now)

        // Then
        assertFalse(firstAppearance)
        assertEquals(BigDecimal("2.30"), progress.easeFactor)
        assertEquals(0, progress.reviewCount)
        assertEquals(1, progress.intervalDays)
        assertEquals(1, progress.incorrectCount)
        assertEquals(TermProgressStatus.LEARNING, progress.status)
        assertEquals(now, progress.lastReviewedAt)
        assertEquals(now.plus(1, ChronoUnit.DAYS), progress.nextReviewAt)
    }

    @Test
    fun `첫 출제 오답 시에도 firstAppearance를 반환한다`() {
        // Given
        val now = Instant.parse("2025-01-01T10:00:00Z")
        val progress = createProgress(
            reviewCount = 0,
            intervalDays = 0,
            easeFactor = BigDecimal("2.50"),
            incorrectCount = 0,
            lastReviewedAt = null
        )

        // When
        val firstAppearance = SrsCalculator.applyAnswer(progress, incorrectEvent(), now)

        // Then
        assertTrue(firstAppearance)
        assertEquals(BigDecimal("2.30"), progress.easeFactor)
        assertEquals(1, progress.incorrectCount)
        assertEquals(TermProgressStatus.LEARNING, progress.status)
    }

    @Test
    fun `오답 시 easeFactor는 MIN_EASE_FACTOR 미만으로 떨어지지 않고 1_30으로 클램프된다`() {
        // Given
        val now = Instant.parse("2025-01-05T10:00:00Z")
        val progress = createProgress(
            reviewCount = 1,
            intervalDays = 1,
            easeFactor = BigDecimal("1.40"),
            lastReviewedAt = Instant.parse("2025-01-04T10:00:00Z")
        )

        // When
        SrsCalculator.applyAnswer(progress, incorrectEvent(), now)

        // Then
        assertEquals(SrsCalculator.MIN_EASE_FACTOR, progress.easeFactor)
    }

    @Test
    fun `이미 MIN_EASE_FACTOR인 단어가 오답이어도 1_30이 유지된다`() {
        // Given
        val now = Instant.parse("2025-01-05T10:00:00Z")
        val progress = createProgress(
            reviewCount = 1,
            intervalDays = 1,
            easeFactor = SrsCalculator.MIN_EASE_FACTOR,
            lastReviewedAt = Instant.parse("2025-01-04T10:00:00Z")
        )

        // When
        SrsCalculator.applyAnswer(progress, incorrectEvent(), now)

        // Then
        assertEquals(SrsCalculator.MIN_EASE_FACTOR, progress.easeFactor)
    }

    @Test
    fun `currentInterval이 0인 단어가 세 번째 정답이어도 intervalDays는 최소 1로 보장되어 계산된다`() {
        // Given
        // reviewCount 2 단계까지 intervalDays 가 0 으로 비정상 누적된 경우를 가정
        val now = Instant.parse("2025-01-05T10:00:00Z")
        val progress = createProgress(
            reviewCount = 2,
            intervalDays = 0,
            easeFactor = BigDecimal("2.50"),
            lastReviewedAt = Instant.parse("2025-01-04T10:00:00Z")
        )

        // When
        SrsCalculator.applyAnswer(progress, correctEvent(), now)

        // Then
        assertEquals(3, progress.reviewCount)
        // currentInterval coerceAtLeast 1 → ceil(1 * 2.50) = 3
        assertEquals(3, progress.intervalDays)
        assertEquals(TermProgressStatus.REVIEW, progress.status)
    }

    private fun createProgress(
        reviewCount: Int,
        intervalDays: Int,
        easeFactor: BigDecimal = BigDecimal("2.50"),
        incorrectCount: Int = 0,
        lastReviewedAt: Instant?,
        status: TermProgressStatus = TermProgressStatus.LEARNING
    ): TermProgressEntity {
        val user = UserEntity(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            socialId = "social-id",
            socialType = SocialType.KAKAO,
            nickname = "learner"
        )
        val glossary = GlossaryEntity(id = 1L, category = "운영체제", title = "프로세스")
        val term = TermEntity(id = 11L, spelling = "context switch", meaning = "문맥 교환")

        return TermProgressEntity(
            id = UUID.fromString("00000000-0000-0000-0000-000000000010"),
            user = user,
            glossary = glossary,
            term = term,
            status = status,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            reviewCount = reviewCount,
            incorrectCount = incorrectCount,
            lastReviewedAt = lastReviewedAt
        )
    }

    private fun correctEvent(): ProgressEvent = ProgressEvent(glossaryId = 1L, termId = 11L, correct = true)
    private fun incorrectEvent(): ProgressEvent = ProgressEvent(glossaryId = 1L, termId = 11L, correct = false)
}
