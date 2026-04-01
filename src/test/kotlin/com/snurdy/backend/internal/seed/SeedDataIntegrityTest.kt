package com.snurdy.backend.internal.seed

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class SeedDataIntegrityTest {

    @Autowired
    private lateinit var glossaryRepository: GlossaryRepository

    @Autowired
    private lateinit var glossaryTermRepository: GlossaryTermRepository

    @Autowired
    private lateinit var termMorphemeRepository: TermMorphemeRepository

    @Test
    fun `term_morphemes position 무결성이 유지되어야 한다`() {
        val anomalies = termMorphemeRepository.findPositionAnomalies().take(10)

        assertTrue(
            anomalies.isEmpty(),
            "term_morphemes position 이상이 있습니다. 샘플=${anomalies.map { "termId=${it.termId}, min=${it.minPosition}, max=${it.maxPosition}, row=${it.rowCount}, distinct=${it.distinctPositionCount}" }}"
        )
    }

    @Test
    fun `샘플 term은 최소 1개 morpheme과 연결되고 참조 무결성을 만족해야 한다`() {
        val samples = termMorphemeRepository.findTermMorphemeCountSamples(20)
        val emptyLinks = samples.filter { it.morphemeCount <= 0L }.map { it.termId }
        val missingReferences = termMorphemeRepository.countMissingMorphemeReferences()

        assertTrue(samples.isNotEmpty(), "term_morphemes 샘플을 찾지 못했습니다.")
        assertTrue(emptyLinks.isEmpty(), "어근어미 연결이 0개인 샘플 term이 있습니다. termIds=$emptyLinks")
        assertEquals(0L, missingReferences, "term_morphemes -> morphemes 참조 무결성이 깨졌습니다. missingCount=$missingReferences")
    }

    @Test
    fun `glossary 매핑 커버리지 정보를 보고 가능한 누락 대상을 파악할 수 있어야 한다`() {
        val glossaryTermCounts = glossaryRepository.findGlossaryTermCounts()
        val totalGlossaryCount = glossaryTermCounts.size
        val mappedGlossaryIds = glossaryTermRepository.findGlossaryIdDistribution().map { it.glossaryId }.sorted()
        val unmappedGlossaries = glossaryTermCounts
            .filter { it.termCount == 0L }
            .map { "${it.glossaryId}:${it.category}/${it.title}" }

        val mappedSet = mappedGlossaryIds.toSet()
        val invalidMappedIds = mappedSet.minus(glossaryTermCounts.map { it.glossaryId }.toSet())

        assertTrue(
            invalidMappedIds.isEmpty(),
            "glossary_terms에 존재하지 않는 glossary_id가 있습니다. ids=$invalidMappedIds"
        )

        assertTrue(
            mappedGlossaryIds.isNotEmpty(),
            "glossary_terms에 매핑된 glossary_id가 없습니다. totalGlossaries=$totalGlossaryCount"
        )

        val termsWithoutGlossary = glossaryTermRepository.findTermsWithoutGlossary(10)
        assertEquals(
            0,
            termsWithoutGlossary.size,
            "glossary에 속하지 않는 term이 존재합니다. count=${termsWithoutGlossary.size}, sample=$termsWithoutGlossary, " +
                "unmappedGlossaries=$unmappedGlossaries, totalGlossaries=$totalGlossaryCount, mappedGlossaryIds=$mappedGlossaryIds"
        )
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
