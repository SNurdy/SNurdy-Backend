package com.snurdy.backend.domain.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.ZoneId

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CoreApiSmokeTest : ApiSmokeTestBase() {

    @Test
    fun `P0 API 스모크 테스트가 인증 사용자 기준으로 동작한다`() {
        val migrationId = startMigration()
        uploadMigration(migrationId)
        mockMvc.post("/v1/migration/$migrationId/finalize") { with(userAuth()) }
            .andExpect { status { isOk() } }

        val termsResponse = mockMvc.get("/v1/dictionary/terms") {
            with(userAuth())
            param("query", "")
            param("page", "0")
            param("size", "5")
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }.andReturn().response.contentAsString

        val firstTermId = json(termsResponse).path("data").path("terms").first().path("id").asLong()

        mockMvc.get("/v1/dictionary/terms/$firstTermId") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.term.id") { value(firstTermId) }
        }

        mockMvc.get("/v1/dictionary/glossaries") {
            with(userAuth())
            param("category", "")
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        mockMvc.get("/v1/dictionary/glossaries/${pairA.glossaryId}/terms") {
            with(userAuth())
            param("filter", "all")
            param("page", "0")
            param("size", "10")
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        mockMvc.get("/v1/bookmarks") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        mockMvc.put("/v1/bookmarks/${pairA.termId}") {
            with(userAuth())
            param("glossaryId", pairA.glossaryId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.bookmarked") { value(true) }
        }

        mockMvc.delete("/v1/bookmarks/${pairA.termId}") {
            with(userAuth())
            param("glossaryId", pairA.glossaryId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.bookmarked") { value(false) }
        }

        val submitPayload = """
            {
              "events": [
                {"glossaryId": ${pairA.glossaryId}, "termId": ${pairA.termId}, "correct": true},
                {"glossaryId": ${pairB.glossaryId}, "termId": ${pairB.termId}, "correct": false}
              ]
            }
        """.trimIndent()

        val year = LocalDate.now(APP_ZONE_ID).year
        val month = LocalDate.now(APP_ZONE_ID).monthValue
        mockMvc.get("/v1/calendar") {
            with(userAuth())
            param("year", year.toString())
            param("month", month.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        mockMvc.get("/v1/users/me/streak") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }
    }

    companion object {
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

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
        }
    }
}
