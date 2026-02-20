package com.snurdy.backend.domain.catalog.controller

import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CatalogApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `단어장 목록 조회는 인증이 필요하다`() {
        mockMvc.get("/v1/catalog/glossaries")
            .andExpect {
                status { isUnauthorized () }
            }
    }

    @Test
    fun `인증 사용자는 단어장 목록을 조회할 수 있다`() {
        mockMvc.get("/v1/catalog/glossaries") {
            with(user("ios-client").roles("USER"))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.glossaries") { isArray() }
                jsonPath("$.data.glossaries.length()") { value(greaterThan(0)) }
            }
    }

    @Test
    fun `인증 사용자는 단어장별 단어 목록을 조회할 수 있다`() {
        mockMvc.get("/v1/catalog/glossaries/4/terms") {
            with(user("ios-client").roles("USER"))
            param("page", "0")
            param("size", "5")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.glossaryId") { value(4) }
                jsonPath("$.data.hasNext") { exists() }
                jsonPath("$.data.terms") { isArray() }
            }
    }

    @Test
    fun `인증 사용자는 단어 상세와 ordered morphemes를 조회할 수 있다`() {
        mockMvc.get("/v1/catalog/terms/3") {
            with(user("ios-client").roles("USER"))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.term.id") { value(3) }
                jsonPath("$.data.glossaries") { isArray() }
                jsonPath("$.data.morphemes") { isArray() }
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
