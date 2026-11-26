package com.snurdy.backend.domain.api

import com.snurdy.backend.global.constants.MigrationError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

// v1 앱은 V52 V55 이전의 옛 termId 를 기기에 들고 있으므로 서버가 대표 termId 로 바꿔 받아야 함
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MigrationTermIdAliasSmokeTest : ApiSmokeTestBase() {

    @Test
    fun `별칭 테이블에 V52 V55 정리 매핑 97건이 최종 대표 ID 로 들어 있다`() {
        val aliasCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM term_id_aliases", Long::class.java)
        assertEquals(97L, aliasCount)

        assertEquals(CANONICAL_ARDS, canonicalOf(ALIAS_ARDS_FIRST))
        assertEquals(CANONICAL_ARDS, canonicalOf(ALIAS_ARDS_SECOND))
        assertEquals(CANONICAL_CONGESTIVE_HEART_FAILURE, canonicalOf(ALIAS_CHAINED_THROUGH_V52))
        assertEquals(CANONICAL_CONGESTIVE_HEART_FAILURE, canonicalOf(ALIAS_DELETED_IN_V55))

        // 한 번만 찾아도 활성 term 이 나와야 하므로 대표 ID 는 별칭이면 안 되고 삭제 상태여도 안 됨
        val chainedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM term_id_aliases a JOIN term_id_aliases b ON b.alias_term_id = a.canonical_term_id",
            Long::class.java
        )
        assertEquals(0L, chainedCount)
        val deletedCanonicalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM term_id_aliases a JOIN terms t ON t.id = a.canonical_term_id WHERE t.deleted_at IS NOT NULL",
            Long::class.java
        )
        assertEquals(0L, deletedCanonicalCount)
    }

    @Test
    fun `soft delete 된 옛 termId 가 섞인 업로드는 대표 termId 로 저장되며 성공한다`() {
        val ardsGlossaryId = findGlossaryIdContaining(CANONICAL_ARDS)
        val heartFailureGlossaryId = findGlossaryIdContaining(CANONICAL_CONGESTIVE_HEART_FAILURE)
        val migrationId = startMigration()

        val response = uploadWithBody(
            migrationId,
            requestBody(
                termProgresses = listOf(
                    termProgressJson(ardsGlossaryId, ALIAS_ARDS_FIRST, reviewCount = 2, lastReviewedAt = "2026-03-01T00:00:00Z"),
                    termProgressJson(heartFailureGlossaryId, ALIAS_CHAINED_THROUGH_V52, reviewCount = 1, lastReviewedAt = "2026-03-01T00:00:00Z")
                ),
                bookmarks = listOf(bookmarkJson(ardsGlossaryId, ALIAS_ARDS_FIRST))
            )
        )

        assertEquals(200, response.status, response.contentAsString)
        assertEquals(2, json(response.contentAsString).path("data").path("termProgressCount").asInt())
        assertEquals(1, json(response.contentAsString).path("data").path("bookmarkCount").asInt())
        assertEquals(
            setOf(CANONICAL_ARDS, CANONICAL_CONGESTIVE_HEART_FAILURE),
            savedTermProgressTermIds().toSet()
        )
        assertEquals(listOf(CANONICAL_ARDS), savedBookmarkTermIds())
        assertEquals(listOf(CANONICAL_ARDS), savedBookmarkNoteTermIds())
    }

    @Test
    fun `옛 ID 와 대표 ID 가 함께 오면 lastReviewedAt 이 최신인 기록 하나만 저장된다`() {
        val glossaryId = findGlossaryIdContaining(CANONICAL_ARDS)
        val migrationId = startMigration()

        val response = uploadWithBody(
            migrationId,
            requestBody(
                termProgresses = listOf(
                    termProgressJson(glossaryId, CANONICAL_ARDS, status = "LEARNING", intervalDays = 1, reviewCount = 9, lastReviewedAt = "2026-01-01T00:00:00Z"),
                    termProgressJson(glossaryId, ALIAS_ARDS_FIRST, status = "MASTERED", intervalDays = 30, reviewCount = 5, lastReviewedAt = "2026-03-01T00:00:00Z"),
                    termProgressJson(glossaryId, ALIAS_ARDS_SECOND, status = "NEW", intervalDays = 0, reviewCount = 0, lastReviewedAt = null)
                ),
                bookmarks = listOf(
                    bookmarkJson(glossaryId, CANONICAL_ARDS),
                    bookmarkJson(glossaryId, ALIAS_ARDS_FIRST),
                    bookmarkJson(glossaryId, ALIAS_ARDS_SECOND)
                )
            )
        )

        assertEquals(200, response.status, response.contentAsString)
        assertEquals(1, json(response.contentAsString).path("data").path("termProgressCount").asInt())
        assertEquals(1, json(response.contentAsString).path("data").path("bookmarkCount").asInt())

        val saved = savedTermProgressRows()
        assertEquals(1, saved.size)
        assertEquals(CANONICAL_ARDS, (saved[0]["term_id"] as Number).toLong())
        assertEquals("MASTERED", saved[0]["status"])
        assertEquals(30, (saved[0]["interval_days"] as Number).toInt())
        assertEquals(5, (saved[0]["review_count"] as Number).toInt())
        assertEquals(listOf(CANONICAL_ARDS), savedBookmarkTermIds())
        assertEquals(listOf(CANONICAL_ARDS), savedBookmarkNoteTermIds())
    }

    @Test
    fun `lastReviewedAt 이 같으면 reviewCount 가 큰 기록이 남고 그래도 같으면 원래 대표 ID 기록이 남는다`() {
        val ardsGlossaryId = findGlossaryIdContaining(CANONICAL_ARDS)
        val atelectasisGlossaryId = findGlossaryIdContaining(CANONICAL_ATELECTASIS)
        val sameTime = "2026-03-01T00:00:00Z"
        val migrationId = startMigration()

        val response = uploadWithBody(
            migrationId,
            requestBody(
                termProgresses = listOf(
                    termProgressJson(ardsGlossaryId, CANONICAL_ARDS, intervalDays = 1, reviewCount = 1, lastReviewedAt = sameTime),
                    termProgressJson(ardsGlossaryId, ALIAS_ARDS_FIRST, intervalDays = 7, reviewCount = 4, lastReviewedAt = sameTime),
                    termProgressJson(atelectasisGlossaryId, ALIAS_ATELECTASIS, intervalDays = 2, reviewCount = 3, lastReviewedAt = sameTime),
                    termProgressJson(atelectasisGlossaryId, CANONICAL_ATELECTASIS, intervalDays = 9, reviewCount = 3, lastReviewedAt = sameTime)
                ),
                bookmarks = emptyList()
            )
        )

        assertEquals(200, response.status, response.contentAsString)
        val intervalByTermId = savedTermProgressRows().associate {
            (it["term_id"] as Number).toLong() to (it["interval_days"] as Number).toInt()
        }
        assertEquals(mapOf(CANONICAL_ARDS to 7, CANONICAL_ATELECTASIS to 9), intervalByTermId)
    }

    @Test
    fun `원본 요청 안의 같은 항목 중복은 별칭 변환과 무관하게 여전히 거절된다`() {
        val glossaryId = findGlossaryIdContaining(CANONICAL_ARDS)
        val migrationId = startMigration()

        val response = uploadWithBody(
            migrationId,
            requestBody(
                termProgresses = listOf(
                    termProgressJson(glossaryId, ALIAS_ARDS_FIRST, reviewCount = 1, lastReviewedAt = "2026-03-01T00:00:00Z"),
                    termProgressJson(glossaryId, ALIAS_ARDS_FIRST, reviewCount = 2, lastReviewedAt = "2026-03-02T00:00:00Z")
                ),
                bookmarks = emptyList()
            )
        )

        assertEquals(400, response.status, response.contentAsString)
        assertEquals("MIGRATION_E009", json(response.contentAsString).path("code").asText())
    }

    @Test
    fun `별칭에도 없는 termId 가 있으면 요청 전체가 거절되고 세션이 FAILED 가 된다`() {
        val glossaryId = findGlossaryIdContaining(CANONICAL_ARDS)
        val migrationId = startMigration()

        val response = uploadWithBody(
            migrationId,
            requestBody(
                termProgresses = listOf(
                    termProgressJson(glossaryId, ALIAS_ARDS_FIRST, reviewCount = 1, lastReviewedAt = "2026-03-01T00:00:00Z"),
                    termProgressJson(glossaryId, UNKNOWN_TERM_ID, reviewCount = 1, lastReviewedAt = "2026-03-01T00:00:00Z")
                ),
                bookmarks = listOf(bookmarkJson(glossaryId, ALIAS_ARDS_FIRST))
            )
        )

        assertEquals(400, response.status, response.contentAsString)
        assertEquals("MIGRATION_E008", json(response.contentAsString).path("code").asText())
        assertEquals(emptyList<Long>(), savedTermProgressTermIds())
        assertEquals(emptyList<Long>(), savedBookmarkTermIds())
        assertEquals("FAILED", sessionStatus())
        assertEquals(INVALID_TERM_ID_MESSAGE, sessionErrorMessage())
    }

    @Test
    fun `대표 termId 가 속하지 않은 단어장과 짝지은 옛 termId 는 조합 오류로 거절된다`() {
        val glossaryId = findGlossaryIdNotContaining(CANONICAL_ARDS)
        val migrationId = startMigration()

        val response = uploadWithBody(
            migrationId,
            requestBody(
                termProgresses = listOf(
                    termProgressJson(glossaryId, ALIAS_ARDS_FIRST, reviewCount = 1, lastReviewedAt = "2026-03-01T00:00:00Z")
                ),
                bookmarks = emptyList()
            )
        )

        assertEquals(400, response.status, response.contentAsString)
        assertEquals("MIGRATION_E012", json(response.contentAsString).path("code").asText())
    }

    @Test
    fun `옛 termId 때문에 FAILED 로 남은 세션에서 다시 업로드하면 성공하고 finalize 까지 진행된다`() {
        val glossaryId = findGlossaryIdContaining(CANONICAL_ARDS)
        val migrationId = startMigration()
        markSessionFailedByInvalidTermId()

        val response = uploadWithBody(
            migrationId,
            requestBody(
                termProgresses = listOf(
                    termProgressJson(glossaryId, ALIAS_ARDS_FIRST, reviewCount = 1, lastReviewedAt = "2026-03-01T00:00:00Z")
                ),
                bookmarks = listOf(bookmarkJson(glossaryId, ALIAS_ARDS_FIRST))
            )
        )

        assertEquals(200, response.status, response.contentAsString)
        assertEquals("UPLOADED", sessionStatus())
        assertEquals(null, sessionErrorMessage())

        mockMvc.post("/v1/migration/$migrationId/finalize") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.status") { value("COMPLETED") }
        }
        mockMvc.get("/v1/migration/status") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.status") { value("COMPLETED") }
        }
    }

    @Test
    fun `서버에 학습 데이터가 있는 사용자는 별칭 변환과 무관하게 여전히 MIGRATION_DATA_CONFLICT 다`() {
        val glossaryId = findGlossaryIdContaining(CANONICAL_ARDS)
        val migrationId = startMigration()
        markSessionFailedByInvalidTermId()
        insertExistingTermProgress()

        val response = uploadWithBody(
            migrationId,
            requestBody(
                termProgresses = listOf(
                    termProgressJson(glossaryId, ALIAS_ARDS_FIRST, reviewCount = 1, lastReviewedAt = "2026-03-01T00:00:00Z")
                ),
                bookmarks = emptyList()
            )
        )

        assertEquals(409, response.status, response.contentAsString)
        assertEquals("MIGRATION_E002", json(response.contentAsString).path("code").asText())
    }

    private fun uploadWithBody(migrationId: String, body: String): MockHttpServletResponse =
        mockMvc.post("/v1/migration/$migrationId/upload") {
            with(userAuth())
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andReturn().response

    private fun requestBody(termProgresses: List<String>, bookmarks: List<String>): String =
        """
        {
          "termProgresses": [${termProgresses.joinToString(",")}],
          "glossaryProgresses": [],
          "bookmarks": [${bookmarks.joinToString(",")}],
          "dailyProgresses": []
        }
        """.trimIndent()

    private fun termProgressJson(
        glossaryId: Long,
        termId: Long,
        status: String = "LEARNING",
        intervalDays: Int = 1,
        reviewCount: Int,
        lastReviewedAt: String?
    ): String {
        val lastReviewedAtJson = lastReviewedAt?.let { "\"$it\"" } ?: "null"
        return """
            {"glossaryId": $glossaryId, "termId": $termId, "status": "$status", "easeFactor": 2.5,
             "intervalDays": $intervalDays, "reviewCount": $reviewCount,
             "lastReviewedAt": $lastReviewedAtJson, "nextReviewAt": null}
        """.trimIndent()
    }

    private fun bookmarkJson(glossaryId: Long, termId: Long): String =
        """{"glossaryId": $glossaryId, "termId": $termId}"""

    private fun canonicalOf(aliasTermId: Long): Long? =
        jdbcTemplate.queryForList(
            "SELECT canonical_term_id FROM term_id_aliases WHERE alias_term_id = ?",
            Long::class.java,
            aliasTermId
        ).firstOrNull()

    private fun findGlossaryIdContaining(termId: Long): Long =
        jdbcTemplate.queryForList(
            "SELECT glossary_id FROM glossary_terms WHERE term_id = ? ORDER BY glossary_id LIMIT 1",
            Long::class.java,
            termId
        ).firstOrNull() ?: error("term $termId 가 속한 단어장이 테스트 DB 에 없습니다")

    private fun findGlossaryIdNotContaining(termId: Long): Long =
        jdbcTemplate.queryForList(
            """
            SELECT g.id FROM glossaries g
            WHERE NOT EXISTS (SELECT 1 FROM glossary_terms gt WHERE gt.glossary_id = g.id AND gt.term_id = ?)
            ORDER BY g.id LIMIT 1
            """.trimIndent(),
            Long::class.java,
            termId
        ).firstOrNull() ?: error("term $termId 가 속하지 않은 단어장이 테스트 DB 에 없습니다")

    private fun savedTermProgressRows(): List<Map<String, Any?>> =
        jdbcTemplate.queryForList(
            "SELECT term_id, status, interval_days, review_count FROM term_progresses WHERE user_id = UUID_TO_BIN(?) ORDER BY term_id",
            userId.toString()
        )

    private fun savedTermProgressTermIds(): List<Long> =
        savedTermProgressRows().map { (it["term_id"] as Number).toLong() }

    private fun savedBookmarkTermIds(): List<Long> =
        jdbcTemplate.queryForList(
            "SELECT term_id FROM bookmarks WHERE user_id = UUID_TO_BIN(?) ORDER BY term_id",
            Long::class.java,
            userId.toString()
        )

    private fun savedBookmarkNoteTermIds(): List<Long> =
        jdbcTemplate.queryForList(
            """
            SELECT cnt.term_id FROM custom_note_terms cnt
            JOIN custom_notes cn ON cn.id = cnt.note_id
            WHERE cn.owner_id = UUID_TO_BIN(?) AND cn.title = '기존 북마크'
            ORDER BY cnt.term_id
            """.trimIndent(),
            Long::class.java,
            userId.toString()
        )

    private fun sessionStatus(): String? =
        jdbcTemplate.queryForList(
            "SELECT status FROM migration_sessions WHERE user_id = UUID_TO_BIN(?)",
            String::class.java,
            userId.toString()
        ).firstOrNull()

    private fun sessionErrorMessage(): String? =
        jdbcTemplate.queryForList(
            "SELECT error_message FROM migration_sessions WHERE user_id = UUID_TO_BIN(?)",
            userId.toString()
        ).firstOrNull()?.get("error_message") as String?

    // 운영의 복구 대상 25건과 같은 상태를 재현
    private fun markSessionFailedByInvalidTermId() {
        jdbcTemplate.update(
            "UPDATE migration_sessions SET status = 'FAILED', error_message = ? WHERE user_id = UUID_TO_BIN(?)",
            INVALID_TERM_ID_MESSAGE,
            userId.toString()
        )
    }

    private fun insertExistingTermProgress() {
        jdbcTemplate.update(
            """
            INSERT INTO term_progresses
            (id, user_id, glossary_id, term_id, status, ease_factor, interval_days, review_count, occurred_at)
            VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, 'LEARNING', 2.50, 1, 1, UTC_TIMESTAMP())
            """.trimIndent(),
            UUID.randomUUID().toString(),
            userId.toString(),
            pairA.glossaryId,
            pairA.termId
        )
    }

    companion object {
        // V52 그룹 2 와 14 그리고 V55 울혈성 심부전 그룹에서 고른 실제 매핑
        private const val CANONICAL_ARDS = 2L
        private const val ALIAS_ARDS_FIRST = 211L
        private const val ALIAS_ARDS_SECOND = 284L
        private const val CANONICAL_ATELECTASIS = 14L
        private const val ALIAS_ATELECTASIS = 296L
        private const val CANONICAL_CONGESTIVE_HEART_FAILURE = 25L
        private const val ALIAS_DELETED_IN_V55 = 224L
        private const val ALIAS_CHAINED_THROUGH_V52 = 744L
        private const val UNKNOWN_TERM_ID = 999_999L
        private val INVALID_TERM_ID_MESSAGE = MigrationError.MIGRATION_INVALID_TERM_ID.message

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