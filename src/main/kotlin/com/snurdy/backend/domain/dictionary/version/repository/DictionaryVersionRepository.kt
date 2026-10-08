package com.snurdy.backend.domain.dictionary.version.repository

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class DictionaryVersionRepository(
    private val entityManager: EntityManager
) {

    // glossary_terms 는 행을 하드 삭제하므로 수정 시각만으로는 삭제를 감지하지 못해 개수를 함께 사용한다
    fun findDictionaryVersion(): String =
        entityManager.createNativeQuery(DICTIONARY_VERSION_QUERY).singleResult as String

    companion object {
        private const val DICTIONARY_VERSION_QUERY = """
            SELECT LEFT(SHA2(CONCAT_WS('|',
                (SELECT COUNT(*) FROM terms WHERE deleted_at IS NULL),
                (SELECT COALESCE(MAX(updated_at), '') FROM terms),
                (SELECT COUNT(*) FROM glossary_terms),
                (SELECT COALESCE(MAX(updated_at), '') FROM glossary_terms),
                (SELECT COUNT(*) FROM glossaries),
                (SELECT COALESCE(MAX(updated_at), '') FROM glossaries)
            ), 256), 16)
        """
    }
}