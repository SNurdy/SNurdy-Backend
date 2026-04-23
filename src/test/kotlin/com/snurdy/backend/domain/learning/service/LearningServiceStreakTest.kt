package com.snurdy.backend.domain.learning.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.dto.LearningAnswerEvent
import com.snurdy.backend.domain.learning.dto.LearningSubmitRequest
import com.snurdy.backend.domain.learning.entity.DailyProgressEntity
import com.snurdy.backend.domain.learning.entity.DailyProgressId
import com.snurdy.backend.domain.learning.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.learning.entity.GlossaryProgressId
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class LearningServiceStreakTest {

    private lateinit var userRepository: UserRepository
    private lateinit var glossaryRepository: GlossaryRepository
    private lateinit var glossaryTermRepository: GlossaryTermRepository
    private lateinit var termRepository: TermRepository
    private lateinit var termProgressRepository: TermProgressRepository
    private lateinit var glossaryProgressRepository: GlossaryProgressRepository
    private lateinit var dailyProgressRepository: DailyProgressRepository
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var clock: Clock

    private lateinit var service: LearningService

    // 고정 시각: 2026-01-15 02:00 UTC == 2026-01-15 11:00 KST (KST 기준 "오늘")
    private val fixedInstant: Instant = Instant.parse("2026-01-15T02:00:00Z")
    private val kst: ZoneId = ZoneId.of("Asia/Seoul")
    private val today: LocalDate = fixedInstant.atZone(kst).toLocalDate()

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        glossaryRepository = mockk()
        glossaryTermRepository = mockk()
        termRepository = mockk()
        termProgressRepository = mockk(relaxed = true)
        glossaryProgressRepository = mockk(relaxed = true)
        dailyProgressRepository = mockk(relaxed = true)
        bookmarkRepository = mockk()
        glossaryTermRepository = mockk()
        clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        service = LearningService(
            userRepository = userRepository,
            glossaryRepository = glossaryRepository,
            glossaryTermRepository = glossaryTermRepository,
            termRepository = termRepository,
            termProgressRepository = termProgressRepository,
            glossaryProgressRepository = glossaryProgressRepository,
            dailyProgressRepository = dailyProgressRepository,
            bookmarkRepository = bookmarkRepository,
            clock = clock
        )
    }

    @Test
    fun `userSummary 없이 마이그레이션된 사용자(streak=0)가 어제 학습 기록이 있는 상태에서 오늘 학습하면 daily_progresses를 역산해 streak가 2가 된다`() {
        val user = freshUser(currentStreak = 0, longestStreak = 0)
        val yesterday = today.minusDays(1)

        // 어제 엔트리: 마이그레이션으로 들어온 활동 (learnedCount=1)
        val yesterdayEntry = DailyProgressEntity(
            id = DailyProgressId(user.id, yesterday),
            user = user,
            learnedCount = 1,
            reviewedCount = 0
        )
        every { dailyProgressRepository.findById(DailyProgressId(user.id, today)) } returns Optional.empty()
        every { dailyProgressRepository.findById(DailyProgressId(user.id, yesterday)) } returns Optional.of(yesterdayEntry)

        // 역산 쿼리: 어제 이전까지 포함해 조회 → 어제만 활성 (그 전날부터는 없음)
        every {
            dailyProgressRepository.findByIdUserIdAndIdDateLessThanEqualOrderByIdDateDesc(user.id, yesterday, any())
        } returns listOf(yesterdayEntry)

        // submit 이벤트 1건: 신규 학습
        every { userRepository.findById(user.id) } returns Optional.of(user)
        stubMinimalSubmitPath(user)

        val captured = slot<DailyProgressEntity>()
        every { dailyProgressRepository.save(capture(captured)) } answers { captured.captured }

        service.submitLearning(
            userId = user.id,
            request = LearningSubmitRequest(events = listOf(dummyEvent()))
        )

        // 오늘 daily 가 신규 생성되어 저장
        assertEquals(today, captured.captured.id.date)
        // 핵심: streak = 어제 역산 1 + 오늘 1 = 2
        assertEquals(2, user.currentStreak, "streak must include yesterday's migrated activity even when userSummary was null")
        assertEquals(2, user.longestStreak)
    }

    @Test
    fun `어제 활동 없고 오늘 처음 학습하면 streak는 1`() {
        val user = freshUser(currentStreak = 0, longestStreak = 0)
        val yesterday = today.minusDays(1)

        every { dailyProgressRepository.findById(DailyProgressId(user.id, today)) } returns Optional.empty()
        every { dailyProgressRepository.findById(DailyProgressId(user.id, yesterday)) } returns Optional.empty()

        every { userRepository.findById(user.id) } returns Optional.of(user)
        stubMinimalSubmitPath(user)
        every { dailyProgressRepository.save(any()) } answers { it.invocation.args[0] as DailyProgressEntity }

        service.submitLearning(
            userId = user.id,
            request = LearningSubmitRequest(events = listOf(dummyEvent()))
        )

        assertEquals(1, user.currentStreak)
    }

    @Test
    fun `오늘 이미 학습한 상태에서 한 번 더 학습해도 streak는 그대로`() {
        val user = freshUser(currentStreak = 3, longestStreak = 5)

        // 오늘 엔트리는 세션 이전부터 이미 활성
        val todayEntry = DailyProgressEntity(
            id = DailyProgressId(user.id, today),
            user = user,
            learnedCount = 1,
            reviewedCount = 1
        )
        every { dailyProgressRepository.findById(DailyProgressId(user.id, today)) } returns Optional.of(todayEntry)

        every { userRepository.findById(user.id) } returns Optional.of(user)
        stubMinimalSubmitPath(user)
        every { dailyProgressRepository.save(any()) } answers { it.invocation.args[0] as DailyProgressEntity }

        service.submitLearning(
            userId = user.id,
            request = LearningSubmitRequest(events = listOf(dummyEvent()))
        )

        assertEquals(3, user.currentStreak)
        assertEquals(5, user.longestStreak)
    }

    // --- helpers ---

    private fun freshUser(currentStreak: Int, longestStreak: Int) = UserEntity(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        socialId = "s-1",
        socialType = SocialType.KAKAO,
        nickname = "tester",
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        totalLearningTerms = 0
    )

    private fun dummyEvent() = LearningAnswerEvent(
        glossaryId = 1L,
        termId = 1L,
        correct = true
    )

    /** processLearningEvent 내부 분기를 통과시키는 최소 mocking. */
    private fun stubMinimalSubmitPath(user: UserEntity) {
        val glossary = GlossaryEntity(id = 1L, category = "c", title = "t")
        val term = TermEntity(id = 1L, spelling = "s", meaning = "m")
        val progress = TermProgressEntity(
            id = UUID.randomUUID(),
            user = user,
            glossary = glossary,
            term = term,
            status = TermProgressStatus.NEW,
            easeFactor = BigDecimal("2.50"),
            intervalDays = 0,
            reviewCount = 0,
            lastReviewedAt = null,
            nextReviewAt = null,
            clientGeneratedId = null,
            occurredAt = fixedInstant
        )
        every { glossaryRepository.findById(1L) } returns Optional.of(glossary)
        every { termRepository.findByIdAndDeletedAtIsNull(1L) } returns Optional.of(term)
        every { glossaryTermRepository.existsByIdGlossaryIdAndIdTermId(1L, 1L) } returns true
        every { termProgressRepository.findByUserIdAndGlossaryIdAndTermId(user.id, 1L, 1L) } returns null
        every { termProgressRepository.save(any<TermProgressEntity>()) } returns progress

        // updateGlossaryProgresses
        every { glossaryProgressRepository.findById(GlossaryProgressId(user.id, 1L)) } returns Optional.empty()
        every { glossaryProgressRepository.save(any<GlossaryProgressEntity>()) } returnsArgument 0
    }
}
