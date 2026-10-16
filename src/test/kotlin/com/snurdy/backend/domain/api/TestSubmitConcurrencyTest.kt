package com.snurdy.backend.domain.api

import com.snurdy.backend.domain.test.common.dto.TestType
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestResultItem
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitRequest
import com.snurdy.backend.domain.test.glossary.service.GlossaryTestSubmitService
import com.snurdy.backend.domain.test.legacy.dto.SubmitRequest
import com.snurdy.backend.domain.test.legacy.dto.TestResultItem
import com.snurdy.backend.domain.test.legacy.entity.TestType as LegacyTestType
import com.snurdy.backend.domain.test.legacy.service.LegacyTestService
import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.domain.user.account.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("local")
class TestSubmitConcurrencyTest {

    @Autowired
    private lateinit var glossaryTestSubmitService: GlossaryTestSubmitService

    @Autowired
    private lateinit var legacyTestService: LegacyTestService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var glossaryId: Long = 0
    private lateinit var termIds: List<Long>

    @BeforeEach
    fun loadGlossaryTerms() {
        glossaryId = jdbcTemplate.queryForObject(
            """
            SELECT glossary_id FROM glossary_terms
             GROUP BY glossary_id HAVING COUNT(*) >= $TERM_COUNT
             ORDER BY glossary_id LIMIT 1
            """.trimIndent(),
            Long::class.java
        )!!
        termIds = jdbcTemplate.queryForList(
            "SELECT term_id FROM glossary_terms WHERE glossary_id = ? ORDER BY term_id LIMIT $TERM_COUNT",
            Long::class.java,
            glossaryId
        )
    }

    @Test
    fun `같은 사용자의 첫 시험 제출이 동시에 들어와도 데드락 없이 모두 처리된다`() {
        repeat(ROUNDS) {
            val userId = createUser()

            val failures = submitConcurrently {
                glossaryTestSubmitService.submitTest(userId, glossaryId, glossaryTestSubmitRequest(), idempotencyKey = null)
            }

            assertEquals(emptyList<String>(), failures, "동시 제출 중 실패한 요청이 있다")
        }
    }

    @Test
    fun `같은 Idempotency-Key 로 동시에 제출하면 학습 기록은 한 번만 반영된다`() {
        repeat(ROUNDS) {
            val userId = createUser()
            val idempotencyKey = UUID.randomUUID()

            val failures = submitConcurrently {
                glossaryTestSubmitService.submitTest(userId, glossaryId, glossaryTestSubmitRequest(), idempotencyKey)
            }

            assertEquals(emptyList<String>(), failures, "동시 제출 중 실패한 요청이 있다")
            assertEquals(1, maxOfUserColumn("term_progresses", "review_count", userId), "정답 단어의 복습 단계가 중복 반영됐다")
            assertEquals(1, maxOfUserColumn("wrong_answer_terms", "total_incorrect_count", userId), "오답 횟수가 중복 반영됐다")
        }
    }

    @Test
    fun `구버전 앱 경로에서도 같은 사용자의 첫 시험 제출이 동시에 들어와도 모두 처리된다`() {
        repeat(ROUNDS) {
            val userId = createUser()

            val failures = submitConcurrently {
                legacyTestService.submitGlossaryTest(userId, glossaryId, legacySubmitRequest())
            }

            assertEquals(emptyList<String>(), failures, "동시 제출 중 실패한 요청이 있다")
        }
    }

    private fun submitConcurrently(submit: () -> Unit): List<String> {
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)
        val ready = CountDownLatch(CONCURRENT_REQUESTS)
        val start = CountDownLatch(1)
        try {
            val futures = (1..CONCURRENT_REQUESTS).map {
                executor.submit<Throwable?> {
                    ready.countDown()
                    start.await()
                    runCatching(submit).exceptionOrNull()
                }
            }
            ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            start.countDown()
            return futures.mapNotNull { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
                .map { error -> generateSequence(error) { it.cause }.joinToString(" <- ") { it.javaClass.simpleName } }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun maxOfUserColumn(table: String, column: String, userId: UUID): Int =
        jdbcTemplate.queryForObject(
            "SELECT MAX($column) FROM $table WHERE user_id = UUID_TO_BIN(?)",
            Int::class.java,
            userId.toString()
        )!!

    private fun createUser(): UUID {
        val userId = UUID.randomUUID()
        userRepository.save(
            UserEntity(
                id = userId,
                socialId = "concurrency-$userId",
                socialType = SocialType.KAKAO,
                nickname = "concurrency"
            )
        )
        return userId
    }

    // 오답이 섞여야 wrong_answer_terms upsert 까지 경합 구간에 들어간다
    private fun glossaryTestSubmitRequest() = GlossaryTestSubmitRequest(
        results = termIds.mapIndexed { index, termId ->
            GlossaryTestResultItem(
                termId = termId,
                testType = TestType.MC_MEANING_FROM_TERM,
                correct = index % 2 == 0,
                userAnswer = "answer"
            )
        },
        studiedTermCount = termIds.size
    )

    private fun legacySubmitRequest() = SubmitRequest(
        results = termIds.mapIndexed { index, termId ->
            TestResultItem(
                termId = termId,
                testType = LegacyTestType.MC_MEANING_FROM_TERM,
                correct = index % 2 == 0,
                userAnswer = "answer"
            )
        },
        studiedTermCount = termIds.size
    )

    companion object {
        private const val TERM_COUNT = 10
        private const val ROUNDS = 5
        private const val CONCURRENT_REQUESTS = 2
        private const val TIMEOUT_SECONDS = 30L

        @Container
        @JvmStatic
        private val mysql = MySQLContainer<Nothing>("mysql:8.0.36").apply {
            withDatabaseName("snurdy_test")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.flyway.enabled") { true }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }

            registry.add("spring.data.redis.host") { "localhost" }
            registry.add("spring.data.redis.port") { 6379 }

            registry.add("jwt.secret-key") { "snurdy-test-secret-key-for-jwt-signing-length-at-least-32" }
            registry.add("jwt.access-token-expiration") { 3600000 }
            registry.add("jwt.admin-access-token-expiration") { 3600000 }
            registry.add("jwt.refresh-token-expiration") { 1209600000 }

            registry.add("oauth.kakao.client-id") { "test-kakao-client" }
            registry.add("oauth.kakao.redirect-uri") { "http://localhost/kakao" }
            registry.add("oauth.apple.team-id") { "test-team" }
            registry.add("oauth.apple.client-id") { "test-apple-client" }
            registry.add("oauth.apple.key-id") { "test-key-id" }
            registry.add("oauth.apple.redirect-uri") { "http://localhost/apple" }
            registry.add("oauth.apple.key-path") { "test-key-path" }
            registry.add("oauth.google.client-id") { "test-google-client" }

            registry.add("inquiry.mail.consumer-enabled") { false }
        }
    }
}