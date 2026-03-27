package com.snurdy.backend.domain.migration.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.learning.repository.DailyProgressRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.migration.dto.MigrationUploadRequest
import com.snurdy.backend.domain.migration.entity.MigrationSessionEntity
import com.snurdy.backend.domain.migration.entity.MigrationStatus
import com.snurdy.backend.domain.migration.repository.MigrationSessionRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.MigrationError
import com.snurdy.backend.global.exception.ApiException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class MigrationServiceTest {

    private lateinit var migrationSessionRepository: MigrationSessionRepository
    private lateinit var termProgressRepository: TermProgressRepository
    private lateinit var glossaryProgressRepository: GlossaryProgressRepository
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var dailyProgressRepository: DailyProgressRepository
    private lateinit var userRepository: UserRepository
    private lateinit var termRepository: TermRepository
    private lateinit var glossaryTermRepository: GlossaryTermRepository
    private lateinit var glossaryRepository: GlossaryRepository
    private lateinit var migrationSessionFailureService: MigrationSessionFailureService

    private lateinit var migrationService: MigrationService

    @BeforeEach
    fun setUp() {
        migrationSessionRepository = mockk()
        termProgressRepository = mockk()
        glossaryProgressRepository = mockk()
        bookmarkRepository = mockk()
        dailyProgressRepository = mockk()
        userRepository = mockk()
        termRepository = mockk()
        glossaryTermRepository = mockk()
        glossaryRepository = mockk()
        migrationSessionFailureService = mockk()

        migrationService = MigrationService(
            migrationSessionRepository = migrationSessionRepository,
            termProgressRepository = termProgressRepository,
            glossaryProgressRepository = glossaryProgressRepository,
            bookmarkRepository = bookmarkRepository,
            dailyProgressRepository = dailyProgressRepository,
            userRepository = userRepository,
            termRepository = termRepository,
            glossaryTermRepository = glossaryTermRepository,
            glossaryRepository = glossaryRepository,
            migrationSessionFailureService = migrationSessionFailureService
        )
    }

    @Test
    fun `start 호출 시 기존 학습 데이터가 있으면 409 충돌을 반환한다`() {
        // given
        every { userRepository.findById(USER_ID) } returns Optional.of(createUser())
        every { migrationSessionRepository.findByUserId(USER_ID) } returns null
        every { termProgressRepository.existsByUserId(USER_ID) } returns true
        every { glossaryProgressRepository.existsByUserId(USER_ID) } returns false
        every { bookmarkRepository.existsByIdUserId(USER_ID) } returns false
        every { dailyProgressRepository.existsByIdUserId(USER_ID) } returns false

        // when
        val exception = assertThrows(ApiException::class.java) {
            migrationService.start(USER_ID)
        }

        // then
        assertEquals(MigrationError.MIGRATION_DATA_CONFLICT, exception.error)
    }

    @Test
    fun `upload 호출 시 세션이 COMPLETED면 기존 반영 건수를 멱등 응답한다`() {
        // given
        val session = createSession(status = MigrationStatus.COMPLETED, completedAt = Instant.parse("2026-03-21T00:00:00Z"))
        val request = MigrationUploadRequest(termProgresses = emptyList(), glossaryProgresses = emptyList())

        every { migrationSessionRepository.findByIdForUpdate(SESSION_ID) } returns session
        every { termProgressRepository.countByUserId(USER_ID) } returns 12L
        every { glossaryProgressRepository.countByUserId(USER_ID) } returns 4L
        every { bookmarkRepository.countByIdUserId(USER_ID) } returns 2L
        every { dailyProgressRepository.countByIdUserId(USER_ID) } returns 3L

        // when
        val response = migrationService.upload(USER_ID, SESSION_ID.toString(), request)

        // then
        assertEquals(SESSION_ID.toString(), response.migrationId)
        assertEquals(12, response.termProgressCount)
        assertEquals(4, response.glossaryProgressCount)
        assertEquals(2, response.bookmarkCount)
        assertEquals(3, response.dailyProgressCount)
    }

    @Test
    fun `finalize 호출 시 세션이 COMPLETED면 멱등 성공 응답한다`() {
        // given
        val completedAt = Instant.parse("2026-03-21T01:00:00Z")
        val session = createSession(status = MigrationStatus.COMPLETED, completedAt = completedAt)

        every { migrationSessionRepository.findByIdForUpdate(SESSION_ID) } returns session
        every { migrationSessionFailureService.markFailed(any(), any()) } just runs

        // when
        val response = migrationService.finalize(USER_ID, SESSION_ID.toString())

        // then
        assertEquals(MigrationStatus.COMPLETED.name, response.status)
        assertEquals(completedAt, response.completedAt)
        verify(exactly = 0) { migrationSessionRepository.save(any()) }
    }

    @Test
    fun `start 호출 시 북마크 데이터가 있으면 409 충돌을 반환한다`() {
        // given
        every { userRepository.findById(USER_ID) } returns Optional.of(createUser())
        every { migrationSessionRepository.findByUserId(USER_ID) } returns null
        every { termProgressRepository.existsByUserId(USER_ID) } returns false
        every { glossaryProgressRepository.existsByUserId(USER_ID) } returns false
        every { bookmarkRepository.existsByIdUserId(USER_ID) } returns true
        every { dailyProgressRepository.existsByIdUserId(USER_ID) } returns false

        // when
        val exception = assertThrows(ApiException::class.java) {
            migrationService.start(USER_ID)
        }

        // then
        assertEquals(MigrationError.MIGRATION_DATA_CONFLICT, exception.error)
    }

    @Test
    fun `start 호출 시 사용자가 마이그레이션을 거부했으면 409 충돌을 반환한다`() {
        // given
        every { userRepository.findById(USER_ID) } returns Optional.of(
            createUser(migrationDeclinedAt = Instant.parse("2026-03-22T00:00:00Z"))
        )

        // when
        val exception = assertThrows(ApiException::class.java) {
            migrationService.start(USER_ID)
        }

        // then
        assertEquals(MigrationError.MIGRATION_DECLINED, exception.error)
    }

    private fun createSession(status: MigrationStatus, completedAt: Instant? = null): MigrationSessionEntity =
        MigrationSessionEntity(
            id = SESSION_ID,
            user = createUser(),
            status = status,
            completedAt = completedAt
        )

    private fun createUser(migrationDeclinedAt: Instant? = null): UserEntity = UserEntity(
        id = USER_ID,
        socialId = "social-id",
        socialType = SocialType.KAKAO,
        nickname = "migrator",
        migrationDeclinedAt = migrationDeclinedAt
    )

    companion object {
        private val USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
        private val SESSION_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000201")
    }
}
