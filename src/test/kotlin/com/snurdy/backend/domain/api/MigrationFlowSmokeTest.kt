package com.snurdy.backend.domain.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MigrationFlowSmokeTest : AbstractP0ApiTestSupport() {

    @Test
    fun `마이그레이션 정상 흐름 start-upload-finalize-status가 동작한다`() {
        val migrationId = startMigration()
        uploadMigration(migrationId)

        mockMvc.get("/v1/migration/status") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.status") { value("UPLOADED") }
        }

        mockMvc.post("/v1/migration/$migrationId/finalize") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.status") { value("COMPLETED") }
        }

        mockMvc.get("/v1/migration/status") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.status") { value("COMPLETED") }
        }
    }

    @Test
    fun `동일 migrationId upload 재시도는 멱등 응답한다`() {
        val migrationId = startMigration()
        uploadMigration(migrationId)

        mockMvc.post("/v1/migration/$migrationId/upload") {
            with(userAuth())
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = migrationPayload()
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.migrationId") { value(migrationId) }
            jsonPath("$.data.termProgressCount") { value(2) }
        }
    }

    @Test
    fun `이미 학습 데이터가 존재하면 start는 409를 반환한다`() {
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

        mockMvc.post("/v1/migration/start") {
            with(userAuth())
        }.andExpect {
            status { isConflict() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.code") { value("MIGRATION_E002") }
        }
    }

    @Test
    fun `앱 강종 재실행 가정 상태 조회가 동작한다`() {
        val migrationId = startMigration()
        uploadMigration(migrationId)

        mockMvc.get("/v1/migration/status") {
            with(userAuth())
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.status") { value("UPLOADED") }
        }
    }

    companion object {
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
