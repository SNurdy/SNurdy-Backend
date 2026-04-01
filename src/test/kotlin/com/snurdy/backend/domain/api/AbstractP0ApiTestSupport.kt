package com.snurdy.backend.domain.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

abstract class AbstractP0ApiTestSupport {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    protected lateinit var userRepository: UserRepository

    protected val userId: UUID = UUID.fromString("00000000-0000-0000-0000-00000000a001")
    protected lateinit var pairA: GlossaryTermPair
    protected lateinit var pairB: GlossaryTermPair

    @BeforeEach
    fun setUpP0TestData() {
        ensureUser()
        loadPairs()
        cleanupUserData()
    }

    protected fun startMigration(): String {
        val response = mockMvc.post("/v1/migration/start") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.status") { value(201) }
        }.andReturn().response.contentAsString

        return json(response).path("data").path("migrationId").asText()
    }

    protected fun uploadMigration(migrationId: String) {
        val response = mockMvc.post("/v1/migration/$migrationId/upload") {
            with(userAuth())
            contentType = MediaType.APPLICATION_JSON
            content = migrationPayload()
        }.andReturn().response

        val body = response.contentAsString
        assertEquals(200, response.status, "upload 호출 상태코드 불일치: $body")
        assertTrue(json(body).path("success").asBoolean(), "upload 호출 실패 응답: $body")
        assertEquals(2, json(body).path("data").path("termProgressCount").asInt(), "termProgressCount 불일치: $body")
        assertEquals(1, json(body).path("data").path("glossaryProgressCount").asInt(), "glossaryProgressCount 불일치: $body")
        assertEquals(1, json(body).path("data").path("bookmarkCount").asInt(), "bookmarkCount 불일치: $body")
        assertEquals(1, json(body).path("data").path("dailyProgressCount").asInt(), "dailyProgressCount 불일치: $body")
    }

    protected fun migrationPayload(): String {
        val now = Instant.now().toString()
        val today = LocalDate.now(ZoneOffset.UTC).toString()

        return """
            {
              "termProgresses": [
                {
                  "glossaryId": ${pairA.glossaryId},
                  "termId": ${pairA.termId},
                  "status": "LEARNING",
                  "easeFactor": 2.5,
                  "intervalDays": 1,
                  "reviewCount": 1,
                  "lastReviewedAt": "$now",
                  "nextReviewAt": "2030-01-01T00:00:00Z"
                },
                {
                  "glossaryId": ${pairB.glossaryId},
                  "termId": ${pairB.termId},
                  "status": "REVIEW",
                  "easeFactor": 2.3,
                  "intervalDays": 3,
                  "reviewCount": 2,
                  "lastReviewedAt": "$now",
                  "nextReviewAt": "2024-01-01T00:00:00Z"
                }
              ],
              "glossaryProgresses": [
                {"glossaryId": ${pairA.glossaryId}, "learnedCount": 1, "lastLearnedAt": "$now"}
              ],
              "bookmarks": [
                {"glossaryId": ${pairA.glossaryId}, "termId": ${pairA.termId}}
              ],
              "dailyProgresses": [
                {"date": "$today", "learnedCount": 1, "reviewedCount": 1}
              ],
              "userSummary": {"currentStreak": 1, "longestStreak": 1, "totalLearningTerms": 1}
            }
        """.trimIndent()
    }

    protected fun userAuth() = authentication(
        UsernamePasswordAuthenticationToken(
            userId,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    )

    protected fun json(content: String): JsonNode = objectMapper.readTree(content)

    private fun ensureUser() {
        val existing = userRepository.findById(userId).orElse(null)
        if (existing != null) return

        userRepository.save(
            UserEntity(
                id = userId,
                socialId = "p0-smoke-user",
                socialType = SocialType.KAKAO,
                nickname = "p0-smoke"
            )
        )
    }

    private fun loadPairs() {
        val pairs = jdbcTemplate.query(
            "SELECT glossary_id, term_id FROM glossary_terms ORDER BY glossary_id, term_id LIMIT 2"
        ) { rs, _ -> GlossaryTermPair(rs.getLong("glossary_id"), rs.getLong("term_id")) }

        require(pairs.size == 2) { "테스트용 glossary_terms 데이터가 부족합니다." }
        pairA = pairs[0]
        pairB = pairs[1]
    }

    private fun cleanupUserData() {
        jdbcTemplate.update("DELETE FROM bookmarks WHERE user_id = UUID_TO_BIN(?)", userId.toString())
        jdbcTemplate.update("DELETE FROM term_progresses WHERE user_id = UUID_TO_BIN(?)", userId.toString())
        jdbcTemplate.update("DELETE FROM glossary_progresses WHERE user_id = UUID_TO_BIN(?)", userId.toString())
        jdbcTemplate.update("DELETE FROM daily_progresses WHERE user_id = UUID_TO_BIN(?)", userId.toString())
        jdbcTemplate.update("DELETE FROM migration_sessions WHERE user_id = UUID_TO_BIN(?)", userId.toString())
        jdbcTemplate.update(
            """
            UPDATE users
               SET current_streak = 0,
                   longest_streak = 0,
                   total_learning_terms = 0,
                   migration_declined_at = NULL
             WHERE id = UUID_TO_BIN(?)
            """.trimIndent(),
            userId.toString()
        )
    }

    protected data class GlossaryTermPair(
        val glossaryId: Long,
        val termId: Long
    )
}
