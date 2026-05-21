package com.snurdy.backend.domain.learning.entity

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class TermProgressEntityTest {

    @Test
    fun `markLearned 호출 시 학습 상태와 복습 메타데이터를 갱신한다`() {
        // Given
        val now = Instant.parse("2025-01-01T10:15:30Z")
        val nextReviewAt = Instant.parse("2025-01-03T10:15:30Z")
        val progress = createTermProgress(
            status = TermProgressStatus.NEW,
            reviewCount = INITIAL_REVIEW_COUNT,
            lastReviewedAt = null,
            nextReviewAt = null
        )

        // When
        progress.markLearned(now = now, nextReviewAt = nextReviewAt)

        // Then
        assertEquals(TermProgressStatus.LEARNING, progress.status)
        assertEquals(INITIAL_REVIEW_COUNT + REVIEW_INCREMENT, progress.reviewCount)
        assertEquals(now, progress.lastReviewedAt)
        assertEquals(nextReviewAt, progress.nextReviewAt)
    }

    @Test
    fun `markLearned 호출 시 기존 상태가 무엇이든 reviewCount는 항상 1 증가한다`() {
        // Given
        val now = Instant.parse("2025-01-10T09:00:00Z")
        val nextReviewAt = Instant.parse("2025-01-11T09:00:00Z")
        val existingReviewCount = 7
        val progress = createTermProgress(
            status = TermProgressStatus.LEARNING,
            reviewCount = existingReviewCount,
            lastReviewedAt = Instant.parse("2025-01-09T09:00:00Z"),
            nextReviewAt = Instant.parse("2025-01-10T09:00:00Z")
        )

        // When
        progress.markLearned(now = now, nextReviewAt = nextReviewAt)

        // Then
        assertEquals(existingReviewCount + REVIEW_INCREMENT, progress.reviewCount)
        assertEquals(TermProgressStatus.LEARNING, progress.status)
        assertEquals(now, progress.lastReviewedAt)
        assertEquals(nextReviewAt, progress.nextReviewAt)
    }

    @Test
    fun `markMastered 호출 시 상태를 MASTERED로 변경하고 다음 복습 일정을 제거한다`() {
        // Given
        val masteredAt = Instant.parse("2025-02-01T00:00:00Z")
        val progress = createTermProgress(
            status = TermProgressStatus.LEARNING,
            reviewCount = 3,
            lastReviewedAt = Instant.parse("2025-01-30T00:00:00Z"),
            nextReviewAt = Instant.parse("2025-02-05T00:00:00Z")
        )

        // When
        progress.markMastered(now = masteredAt)

        // Then
        assertEquals(TermProgressStatus.MASTERED, progress.status)
        assertEquals(masteredAt, progress.lastReviewedAt)
        assertNull(progress.nextReviewAt)
        assertEquals(3, progress.reviewCount)
    }

    @Test
    fun `TermProgressEntity 기본 생성 시 도메인 기본값을 사용한다`() {
        // Given
        val user = createUser()
        val glossary = GlossaryEntity(id = 1L, category = "네트워크", title = "TCP")
        val term = TermEntity(id = 10L, spelling = "SYN", meaning = "연결 요청")

        // When
        val progress = TermProgressEntity(
            user = user,
            glossary = glossary,
            term = term
        )

        // Then
        assertEquals(TermProgressStatus.NEW, progress.status)
        assertEquals(BigDecimal("2.50"), progress.easeFactor)
        assertEquals(0, progress.intervalDays)
        assertEquals(0, progress.reviewCount)
        assertNull(progress.lastReviewedAt)
        assertNull(progress.nextReviewAt)
    }

    private fun createTermProgress(
        status: TermProgressStatus,
        reviewCount: Int,
        lastReviewedAt: Instant?,
        nextReviewAt: Instant?
    ): TermProgressEntity {
        val user = createUser()
        val glossary = GlossaryEntity(id = 1L, category = "운영체제", title = "프로세스")
        val term = TermEntity(id = 11L, spelling = "context switch", meaning = "문맥 교환")

        return TermProgressEntity(
            id = UUID.fromString("00000000-0000-0000-0000-000000000120"),
            user = user,
            glossary = glossary,
            term = term,
            status = status,
            reviewCount = reviewCount,
            lastReviewedAt = lastReviewedAt,
            nextReviewAt = nextReviewAt
        )
    }

    private fun createUser(): UserEntity = UserEntity(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        socialId = "social-id",
        socialType = SocialType.KAKAO,
        nickname = "learner"
    )

    companion object {
        private const val INITIAL_REVIEW_COUNT = 0
        private const val REVIEW_INCREMENT = 1
    }
}
