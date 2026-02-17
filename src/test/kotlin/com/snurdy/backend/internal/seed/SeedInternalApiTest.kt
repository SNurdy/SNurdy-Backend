package com.snurdy.backend.internal.seed

import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SeedInternalApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `summary API는 200과 핵심 집계 필드를 반환해야 한다`() {
        mockMvc.get("/internal/seed/summary")
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.tableCounts.glossaries") { value(greaterThan(0)) }
                jsonPath("$.data.tableCounts.terms") { value(greaterThan(0)) }
                jsonPath("$.data.glossaryDistribution.mappedGlossaryIds") { isArray() }
                jsonPath("$.data.glossaryDistribution.unmappedGlossaries") { isArray() }
            }
    }

    @Test
    fun `anomalies API는 200과 이상징후 필드를 반환해야 한다`() {
        mockMvc.get("/internal/seed/anomalies")
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.glossariesWithoutMappings") { isArray() }
                jsonPath("$.data.termsWithoutGlossary") { isArray() }
                jsonPath("$.data.termMorphemePositionAnomalies") { isArray() }
                jsonPath("$.data.orphanMorphemes.count") { value(greaterThan(-1)) }
                jsonPath("$.data.orphanMorphemes.samples") { isArray() }
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
